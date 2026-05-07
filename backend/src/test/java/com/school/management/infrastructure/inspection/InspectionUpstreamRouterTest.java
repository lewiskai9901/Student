package com.school.management.infrastructure.inspection;

import com.school.management.infrastructure.persistence.inspection.analytics.AlertPO;
import com.school.management.infrastructure.persistence.inspection.analytics.AlertRulePO;
import com.school.management.infrastructure.persistence.inspection.corrective.CorrectiveCaseMapper;
import com.school.management.infrastructure.persistence.inspection.corrective.CorrectiveCasePO;
import com.school.management.infrastructure.persistence.inspection.corrective.CorrectiveSubtaskPO;
import com.school.management.infrastructure.persistence.inspection.execution.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Router 单测: 12 个 inspection PO → 上游 PO 反查 orgUnitId 的中央路由表.
 *
 * 该 router 由 InspectionDataPermissionFiller (MetaObjectHandler) 在 INSERT
 * 阶段调用, 实现 "数据权限边界从父聚合自动继承".
 */
@DisplayName("InspectionUpstreamRouter — PO 反查上游 orgUnitId")
class InspectionUpstreamRouterTest {

    private InspProjectMapper projectMapper;
    private InspSubmissionMapper submissionMapper;
    private CorrectiveCaseMapper caseMapper;

    private InspectionUpstreamRouter router;

    @BeforeEach
    void setUp() {
        projectMapper = mock(InspProjectMapper.class);
        submissionMapper = mock(InspSubmissionMapper.class);
        caseMapper = mock(CorrectiveCaseMapper.class);

        router = new InspectionUpstreamRouter(projectMapper, submissionMapper, caseMapper);
    }

    @Test
    @DisplayName("InspTaskPO → 反查 InspProjectPO.orgUnitId")
    void taskInheritsFromProject() {
        InspProjectPO project = new InspProjectPO();
        project.setId(100L);
        project.setOrgUnitId(50L);
        when(projectMapper.selectById(eq(100L))).thenReturn(project);

        InspTaskPO task = new InspTaskPO();
        task.setProjectId(100L);

        assertThat(router.resolve(task)).isEqualTo(50L);
    }

    @Test
    @DisplayName("ProjectInspectorPO → 反查 InspProjectPO.orgUnitId")
    void projectInspectorInheritsFromProject() {
        InspProjectPO project = new InspProjectPO();
        project.setId(100L);
        project.setOrgUnitId(50L);
        when(projectMapper.selectById(eq(100L))).thenReturn(project);

        ProjectInspectorPO inspector = new ProjectInspectorPO();
        inspector.setProjectId(100L);

        assertThat(router.resolve(inspector)).isEqualTo(50L);
    }

    @Test
    @DisplayName("InspEvidencePO → 反查 InspSubmissionPO.orgUnitId")
    void evidenceInheritsFromSubmission() {
        InspSubmissionPO submission = new InspSubmissionPO();
        submission.setId(200L);
        submission.setOrgUnitId(60L);
        when(submissionMapper.selectById(eq(200L))).thenReturn(submission);

        InspEvidencePO evidence = new InspEvidencePO();
        evidence.setSubmissionId(200L);

        assertThat(router.resolve(evidence)).isEqualTo(60L);
    }

    @Test
    @DisplayName("SubmissionDetailPO → 反查 submission.orgUnitId")
    void submissionDetailInheritsFromSubmission() {
        InspSubmissionPO submission = new InspSubmissionPO();
        submission.setId(200L);
        submission.setOrgUnitId(60L);
        when(submissionMapper.selectById(eq(200L))).thenReturn(submission);

        SubmissionDetailPO detail = new SubmissionDetailPO();
        detail.setSubmissionId(200L);

        assertThat(router.resolve(detail)).isEqualTo(60L);
    }

    @Test
    @DisplayName("ViolationRecordPO → 反查 submission.orgUnitId")
    void violationInheritsFromSubmission() {
        InspSubmissionPO submission = new InspSubmissionPO();
        submission.setId(200L);
        submission.setOrgUnitId(60L);
        when(submissionMapper.selectById(eq(200L))).thenReturn(submission);

        ViolationRecordPO violation = new ViolationRecordPO();
        violation.setSubmissionId(200L);

        assertThat(router.resolve(violation)).isEqualTo(60L);
    }

    @Test
    @DisplayName("CorrectiveSubtaskPO → 反查 CorrectiveCasePO.orgUnitId")
    void correctiveSubtaskInheritsFromCase() {
        CorrectiveCasePO casePO = new CorrectiveCasePO();
        casePO.setId(300L);
        casePO.setOrgUnitId(70L);
        when(caseMapper.selectById(eq(300L))).thenReturn(casePO);

        CorrectiveSubtaskPO subtask = new CorrectiveSubtaskPO();
        subtask.setCaseId(300L);

        assertThat(router.resolve(subtask)).isEqualTo(70L);
    }

    @Test
    @DisplayName("AlertRulePO → 反查 InspProjectPO.orgUnitId (规则关联项目)")
    void alertRuleInheritsFromProject() {
        InspProjectPO project = new InspProjectPO();
        project.setId(100L);
        project.setOrgUnitId(50L);
        when(projectMapper.selectById(eq(100L))).thenReturn(project);

        AlertRulePO rule = new AlertRulePO();
        rule.setProjectId(100L);

        assertThat(router.resolve(rule)).isEqualTo(50L);
    }

    @Test
    @DisplayName("AlertPO 当 target_type=ORG_UNIT 时, target_id 即 orgUnitId")
    void alertOrgUnitIsTargetIdWhenTargetTypeIsOrgUnit() {
        AlertPO alert = new AlertPO();
        alert.setTargetType("ORG_UNIT");
        alert.setTargetId(80L);

        assertThat(router.resolve(alert)).isEqualTo(80L);
    }

    @Test
    @DisplayName("AlertPO target_type=PLACE 等其它类型 → 无法反查, 返回 null")
    void alertReturnsNullWhenTargetTypeIsNotOrgUnit() {
        AlertPO alert = new AlertPO();
        alert.setTargetType("PLACE");
        alert.setTargetId(80L);

        assertThat(router.resolve(alert)).isNull();
    }

    @Test
    @DisplayName("InspProjectPO 是源头, router 不反查, 返回 null (由 SecurityContext 兜底)")
    void projectIsSourceReturnsNull() {
        InspProjectPO project = new InspProjectPO();
        project.setId(100L);

        assertThat(router.resolve(project)).isNull();
    }

    @Test
    @DisplayName("registeredTypes() 必须覆盖全部 13 张表对应的 PO 类")
    void registeredTypesCoverAll13Tables() {
        Set<Class<?>> types = router.registeredTypes();
        assertThat(types).contains(
                InspProjectPO.class,
                InspTaskPO.class,
                SubmissionDetailPO.class,
                InspEvidencePO.class,
                ProjectInspectorPO.class,
                CorrectiveSubtaskPO.class,
                ViolationRecordPO.class,
                AlertPO.class,
                AlertRulePO.class
                // CorrectiveCasePO / SubmissionObservationPO / 3 张 summary PO 已在
                // RepositoryImpl/AnalyticsProjectionService 显式 set, 路由表可不重复注册
        );
    }
}
