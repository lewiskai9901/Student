package com.school.management.infrastructure.inspection;

import com.school.management.infrastructure.access.UserContext;
import com.school.management.infrastructure.access.UserContextHolder;
import com.school.management.infrastructure.metrics.InspectionMetrics;
import com.school.management.infrastructure.persistence.inspection.corrective.CorrectiveCaseMapper;
import com.school.management.infrastructure.persistence.inspection.execution.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MetaObjectHandler 行为守护:
 *   1. INSERT 时若 PO 是已注册类型 + orgUnitId 为 null → 反查上游注入
 *   2. 反查不到 → SecurityContext.currentUser.orgUnitId 兜底
 *   3. orgUnitId 已被业务层显式 set → 保留, 不覆盖
 *   4. UPDATE 永远不动 orgUnitId
 *   5. PO 类型未注册 → 不动 (避免污染非 inspection 表)
 */
@DisplayName("InspectionDataPermissionFiller — orgUnitId 自动填充")
class InspectionDataPermissionFillerTest {

    private InspProjectMapper projectMapper;
    private InspSubmissionMapper submissionMapper;
    private CorrectiveCaseMapper caseMapper;
    private InspectionUpstreamRouter router;
    private MeterRegistry meterRegistry;
    private InspectionMetrics metrics;
    private InspectionDataPermissionFiller filler;

    @BeforeEach
    void setUp() {
        projectMapper = mock(InspProjectMapper.class);
        submissionMapper = mock(InspSubmissionMapper.class);
        caseMapper = mock(CorrectiveCaseMapper.class);
        router = new InspectionUpstreamRouter(projectMapper, submissionMapper, caseMapper);
        meterRegistry = new SimpleMeterRegistry();
        metrics = new InspectionMetrics(meterRegistry);
        metrics.init();  // @PostConstruct 手动触发 (单测无 Spring 上下文)
        filler = new InspectionDataPermissionFiller(router, metrics);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("InspTask 上游可反查时 — 注入 project.orgUnitId")
    void insertFillsTaskFromUpstreamProject() {
        InspProjectPO project = new InspProjectPO();
        project.setId(100L);
        project.setOrgUnitId(50L);
        when(projectMapper.selectById(100L)).thenReturn(project);

        InspTaskPO task = new InspTaskPO();
        task.setProjectId(100L);

        filler.insertFill(SystemMetaObject.forObject(task));

        assertThat(task.getOrgUnitId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("InspProject 是源头 — 反查无果, 走 SecurityContext 兜底")
    void insertFillsProjectFromSecurityContext() {
        UserContextHolder.setContext(buildUserContext(999L, 70L));

        InspProjectPO project = new InspProjectPO();
        project.setProjectCode("PRJ-1");

        filler.insertFill(SystemMetaObject.forObject(project));

        assertThat(project.getOrgUnitId()).isEqualTo(70L);
    }

    @Test
    @DisplayName("orgUnitId 已被显式 set — 不覆盖业务层赋值 (override 通道)")
    void insertDoesNotOverrideExplicitOrgUnitId() {
        UserContextHolder.setContext(buildUserContext(999L, 70L));
        InspProjectPO project = new InspProjectPO();
        project.setProjectCode("PRJ-1");
        project.setOrgUnitId(123L);  // 业务层显式赋值

        filler.insertFill(SystemMetaObject.forObject(project));

        assertThat(project.getOrgUnitId()).isEqualTo(123L);  // 保留, 不被 70L 覆盖
    }

    @Test
    @DisplayName("PO 类型未注册 — handler 不动它")
    void insertIgnoresUnregisteredPoTypes() {
        // InspectionPlanPO 当前未在路由表注册
        // (确认: 只有 13 张 inspection_data_permission_columns 表的 PO 在路由表)
        com.school.management.infrastructure.persistence.inspection.execution.InspectionPlanPO plan =
                new com.school.management.infrastructure.persistence.inspection.execution.InspectionPlanPO();
        plan.setPlanName("X");

        // 即使 SecurityContext 有值, 未注册的 PO 也不应被填
        UserContextHolder.setContext(buildUserContext(999L, 70L));

        filler.insertFill(SystemMetaObject.forObject(plan));

        // InspectionPlanPO 没有 orgUnitId 字段, getValue 会失败 — 这里只断言无异常
        // (handler 内必须先用 hasGetter 判断, 不能瞎写)
    }

    @Test
    @DisplayName("UPDATE 永远不动 orgUnitId — 即便当前值为 null")
    void updateNeverFillsOrgUnitId() {
        UserContextHolder.setContext(buildUserContext(999L, 70L));

        InspProjectPO project = new InspProjectPO();
        project.setProjectCode("PRJ-1");
        project.setOrgUnitId(null);

        filler.updateFill(SystemMetaObject.forObject(project));

        assertThat(project.getOrgUnitId()).isNull();
    }

    @Test
    @DisplayName("反查上游 + SecurityContext 都失败 — 静默 (后续 ArchUnit/metric 兜底)")
    void insertSilentlyDoesNothingWhenAllSourcesAreNull() {
        // projectMapper 返回 null (上游不存在), 也无 SecurityContext

        InspTaskPO task = new InspTaskPO();
        task.setProjectId(100L);
        when(projectMapper.selectById(100L)).thenReturn(null);

        filler.insertFill(SystemMetaObject.forObject(task));

        assertThat(task.getOrgUnitId()).isNull();
    }

    @Test
    @DisplayName("Metric: upstream 命中 → counter strategy=upstream +1")
    void metricCountsUpstreamHit() {
        InspProjectPO project = new InspProjectPO();
        project.setId(100L);
        project.setOrgUnitId(50L);
        when(projectMapper.selectById(100L)).thenReturn(project);

        InspTaskPO task = new InspTaskPO();
        task.setProjectId(100L);
        filler.insertFill(SystemMetaObject.forObject(task));

        assertThat(meterRegistry.counter("inspection_orgunit_filled_total", "strategy", "upstream").count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("Metric: SecurityContext 命中 → counter strategy=context +1")
    void metricCountsContextHit() {
        UserContextHolder.setContext(buildUserContext(999L, 70L));
        InspProjectPO project = new InspProjectPO();
        filler.insertFill(SystemMetaObject.forObject(project));

        assertThat(meterRegistry.counter("inspection_orgunit_filled_total", "strategy", "context").count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("Metric: 全空 fallback 失败 → counter strategy=missed +1 (生产监控漏率)")
    void metricCountsMissedHit() {
        InspTaskPO task = new InspTaskPO();
        task.setProjectId(100L);
        when(projectMapper.selectById(100L)).thenReturn(null);

        filler.insertFill(SystemMetaObject.forObject(task));

        assertThat(meterRegistry.counter("inspection_orgunit_filled_total", "strategy", "missed").count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("Metric: 业务层显式 set → counter strategy=existing +1")
    void metricCountsExistingHit() {
        InspProjectPO project = new InspProjectPO();
        project.setOrgUnitId(123L);
        filler.insertFill(SystemMetaObject.forObject(project));

        assertThat(meterRegistry.counter("inspection_orgunit_filled_total", "strategy", "existing").count())
                .isEqualTo(1.0);
    }

    private UserContext buildUserContext(Long userId, Long orgUnitId) {
        return UserContext.builder().userId(userId).orgUnitId(orgUnitId).build();
    }
}
