package com.school.management.infrastructure.access;

import com.school.management.application.access.DynamicModuleService;
import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.valueobject.ScopeSpec;
import com.school.management.infrastructure.persistence.access.DataModulePO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 插件维度 (BY_CLASS/BY_GRADE/...) 路径的拦截器行为测试 — Casbin 缺口 E2E 钓出的两个真 bug:
 *
 * <ol>
 *   <li><b>resourceType 空串</b>: {@code @DataPermission.resourceType} 默认 "",
 *       moduleConfig.resourceType 映射自 data_resources.access_resource_type (另一语义,
 *       student/school_class 等均为 NULL) → 传给 resolver 的 resourceType=""
 *       → ClassDataScopeResolver "resourceType '' not supported" → 一律降级 SELF。
 *       兜底链最后一环必须落到 moduleCode 本身 (resolver 的 switch case 就是
 *       data_resources 资源码: student/school_class/attendance...)。</li>
 *   <li><b>降级 SELF 不识 viaMembership</b>: buildPluginDimCondition 的 null 分支
 *       直接拼 {@code creatorField = ?}。对 viaMembership 主表 (user_student 无
 *       created_by 列, V20260531 已 DROP) 直接 SQL 报错 500。必须与静态 SELF 路径
 *       同语义: 短路到 {@code membershipSubjectColumn = ?} (s.user_id = 当前用户)。</li>
 * </ol>
 *
 * 测试基建镜像 {@link DataPermissionInterceptorMembershipCustomTest}。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataPermissionInterceptorPluginDimTest {

    @Mock
    private DynamicModuleService dynamicModuleService;

    @Mock
    private DataPermissionPolicyService dataPermissionPolicyService;

    @Mock
    private PluginDataScopeRouter pluginDataScopeRouter;

    private DataPermissionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new DataPermissionInterceptor();
        ReflectionTestUtils.setField(interceptor, "dynamicModuleService", dynamicModuleService);
        ReflectionTestUtils.setField(interceptor, "dataPermissionPolicyService", dataPermissionPolicyService);
        // Tier 1: buildMeta 锚点改由 resourceRelationRegistry 驱动 (注解锚兜底已删)。
        // student=成员图 (viaMembership), attendance=列锚 (creator=recorded_by)。
        ResourceRelationRegistry resourceRelationRegistry = mock(ResourceRelationRegistry.class);
        when(resourceRelationRegistry.forResource(anyString()))
                .thenReturn(Optional.of(new ResourceRelationRegistry.DerivedAnchor(false, "org_unit_id", "created_by")));
        when(resourceRelationRegistry.forResource("student"))
                .thenReturn(Optional.of(new ResourceRelationRegistry.DerivedAnchor(true, null, null)));
        when(resourceRelationRegistry.forResource("attendance"))
                .thenReturn(Optional.of(new ResourceRelationRegistry.DerivedAnchor(false, "org_unit_id", "recorded_by")));
        ReflectionTestUtils.setField(interceptor, "resourceRelationRegistry", resourceRelationRegistry);
        // T7/R4: 插件维度 compose 下沉 ScopeEvaluator (持 router + registry); 拦截器只编排。
        ReflectionTestUtils.setField(interceptor, "scopeEvaluator",
                new ScopeEvaluator(pluginDataScopeRouter, resourceRelationRegistry, org.mockito.Mockito.mock(RecordRelationResolverRouter.class)));
        UserContextHolder.clear();
        UserContextHolder.enableDataPermission();
    }

    /** PLUGIN_DIM 规格 (anchorParam = dimCode), 替代旧 getScopeCodeForRole 返回原始码。 */
    private ScopeSpec pluginDimSpec(String dimCode) {
        return ScopeSpec.builder().orgAnchor(OrgAnchor.PLUGIN_DIM).anchorParam(dimCode).build();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    // ── fixtures ──

    @DataPermission(module = "student", tableAlias = "s",
            membershipSubjectColumn = "user_id")
    interface StudentMembershipMapper {
        List<Object> selectList();
    }

    private DataPermission studentAnnotation() {
        return StudentMembershipMapper.class.getAnnotation(DataPermission.class);
    }

    /** 真实形态: data_resources.access_resource_type=NULL → moduleConfig.resourceType=null */
    private DataModulePO moduleConfigWithNullResourceType() {
        DataModulePO po = new DataModulePO();
        po.setModuleCode("student");
        po.setEnabled(true);
        po.setResourceType(null);
        return po;
    }

    private UserContext classTeacherCtx(Long roleId) {
        UserContext.ScopedRoleInfo role = UserContext.ScopedRoleInfo.builder()
                .roleId(roleId).scopeType("ALL").scopeId(0L).build();
        return UserContext.builder()
                .userId(42L).username("dpt_ct")
                .orgUnitId(100L).orgUnitPath("1.10.100.")
                .roleIds(List.of(roleId)).tenantId(1L)
                .scopedRoles(List.of(role))
                .build();
    }

    private Object build(DataPermission ann, DataModulePO module, UserContext ctx) {
        return ReflectionTestUtils.invokeMethod(interceptor, "buildScopedCondition",
                ann, module, ctx, 1L, "READ");
    }

    private String sqlOf(Object cond) {
        return (String) ReflectionTestUtils.getField(cond, "sql");
    }

    @Test
    @DisplayName("resourceType 空时必须兜底到 moduleCode 传给 resolver (而非空串)")
    void pluginDim_fallsBackToModuleCode_whenResourceTypeBlank() {
        UserContext ctx = classTeacherCtx(7L);
        when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(7L), anyString(), anyString()))
                .thenReturn(pluginDimSpec("BY_CLASS"));
        when(pluginDataScopeRouter.resolve(eq("BY_CLASS"), eq(42L), anyString()))
                .thenReturn(List.of(3001L, 3002L));

        build(studentAnnotation(), moduleConfigWithNullResourceType(), ctx);

        // 核心断言: resolver 收到的是 "student" (moduleCode), 不是 ""
        verify(pluginDataScopeRouter).resolve("BY_CLASS", 42L, "student");
    }

    @Test
    @DisplayName("resolver 命中 → s.id IN (ids)")
    void pluginDim_resolved_filtersByIdList() {
        UserContext ctx = classTeacherCtx(7L);
        when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(7L), anyString(), anyString()))
                .thenReturn(pluginDimSpec("BY_CLASS"));
        when(pluginDataScopeRouter.resolve(eq("BY_CLASS"), eq(42L), anyString()))
                .thenReturn(List.of(3001L, 3002L));

        Object cond = build(studentAnnotation(), moduleConfigWithNullResourceType(), ctx);

        assertThat(sqlOf(cond)).contains("s.id IN (3001,3002)");
    }

    @Test
    @DisplayName("resolver 降级 SELF + viaMembership → s.user_id = ? (不得拼已 DROP 的 created_by)")
    void pluginDim_degradeSelf_respectsMembershipSubjectColumn() {
        UserContext ctx = classTeacherCtx(7L);
        when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(7L), anyString(), anyString()))
                .thenReturn(pluginDimSpec("BY_CLASS"));
        // resolver 不可用 → 降级 SELF
        when(pluginDataScopeRouter.resolve(eq("BY_CLASS"), eq(42L), anyString()))
                .thenReturn(null);

        Object cond = build(studentAnnotation(), moduleConfigWithNullResourceType(), ctx);

        String sql = sqlOf(cond);
        assertThat(sql)
                .as("viaMembership 主表的 SELF = 行本身的 subject 列 (s.user_id), 与静态 SELF 路径同语义")
                .contains("s.user_id = ?");
        assertThat(sql)
                .as("user_student.created_by 已 DROP, 拼进去直接 SQL 报错")
                .doesNotContain("created_by");
    }

    @Test
    @DisplayName("resolver 降级 SELF + 非 membership 表 → 仍走 creatorField (行为不回归)")
    void pluginDim_degradeSelf_nonMembership_usesCreatorField() {
        UserContext ctx = classTeacherCtx(7L);
        when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(7L), anyString(), anyString()))
                .thenReturn(pluginDimSpec("BY_CLASS"));
        when(pluginDataScopeRouter.resolve(eq("BY_CLASS"), eq(42L), anyString()))
                .thenReturn(null);

        Object cond = build(plainAnnotation(), plainModuleConfig(), ctx);

        assertThat(sqlOf(cond)).contains("t.recorded_by = ?");
    }

    @DataPermission(module = "attendance", tableAlias = "t")
    interface PlainMapper {
        List<Object> selectList();
    }

    private DataPermission plainAnnotation() {
        return PlainMapper.class.getAnnotation(DataPermission.class);
    }

    private DataModulePO plainModuleConfig() {
        DataModulePO po = new DataModulePO();
        po.setModuleCode("attendance");
        po.setEnabled(true);
        po.setResourceType(null);
        return po;
    }
}
