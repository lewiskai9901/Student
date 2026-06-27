package com.school.management.infrastructure.access;

import com.school.management.application.access.DynamicModuleService;
import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.RoleAssignmentScope;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Focused tests for the CUSTOM_ORG membership path (post-T7 cutover).
 *
 * <p>Drives the interceptor's {@code buildScopedCondition} (via reflection) with a
 * reflectively-injected real {@link ScopeEvaluator}; {@code getScopeSpec} is stubbed to
 * return a CUSTOM_ORG {@link ScopeSpec}, so the actual SQL is composed by the evaluator's
 * membership path. (The old private {@code buildMembershipCondition} was deleted in T7 —
 * its CUSTOM logic now lives in {@code ScopeEvaluator.membershipSelect}.)
 *
 * <p>Bug under test: granting CUSTOM scope = a GRADE (or department) org used to emit
 * {@code ar.resource_id IN (<rawGrantedIds>)}, which matches ZERO student members
 * because students are members of the grade's CHILD class orgs, not the grade itself.
 * The CUSTOM membership branch must subtree-expand each granted org id (org + all
 * descendants) before matching member tuples — mirroring {@code buildCustomCondition}'s
 * {@code tree_path LIKE CONCAT(g.tree_path, '%')} pattern.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataPermissionInterceptorMembershipCustomTest {

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
        // Tier 1: buildMeta 锚点改由 resourceRelationRegistry 驱动 (注解锚兜底已删)。student=成员图。
        ResourceRelationRegistry resourceRelationRegistry = mock(ResourceRelationRegistry.class);
        when(resourceRelationRegistry.forResource(anyString()))
                .thenReturn(Optional.of(new ResourceRelationRegistry.DerivedAnchor(true, null, null, "user_id")));  // Tier2: subject 列归注册表
        ReflectionTestUtils.setField(interceptor, "resourceRelationRegistry", resourceRelationRegistry);
        // T7/R4: CUSTOM membership compose 下沉 ScopeEvaluator (持 router + registry); 拦截器只编排。
        ReflectionTestUtils.setField(interceptor, "scopeEvaluator",
                new ScopeEvaluator(pluginDataScopeRouter, resourceRelationRegistry, org.mockito.Mockito.mock(RecordRelationResolverRouter.class),
                        new ChainCompiler(new ChainHopResolver(), resourceRelationRegistry)));
        UserContextHolder.clear();
        UserContextHolder.enableDataPermission();
    }

    /** CUSTOM_ORG 规格 (customOrgIds = 指定组织集)。 */
    private ScopeSpec customOrgSpec(Set<Long> ids) {
        return ScopeSpec.builder().orgAnchor(OrgAnchor.CUSTOM_ORG).customOrgIds(ids).build();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    // ── fixtures (copied from sibling test to stay isolated) ──

    @DataPermission(module = "student", tableAlias = "s")  // Tier2: subject 列 user_id 由注册表 mock 提供
    interface StudentMembershipMapper {
        List<Object> selectList();
    }

    private DataPermission studentAnnotation() {
        return StudentMembershipMapper.class.getAnnotation(DataPermission.class);
    }

    private DataModulePO moduleConfig(String resourceType) {
        DataModulePO po = new DataModulePO();
        po.setModuleCode("student");
        po.setEnabled(true);
        po.setResourceType(resourceType);
        return po;
    }

    private UserContext.ScopedRoleInfo scopedRole(Long roleId, String scopeType, Long scopeId, String orgPath) {
        return UserContext.ScopedRoleInfo.builder()
                .roleId(roleId)
                .scopeType(scopeType)
                .scopeId(scopeId)
                .scopeOrgPath(orgPath)
                .build();
    }

    private UserContext userWithScopedRoles(List<UserContext.ScopedRoleInfo> roles) {
        return UserContext.builder()
                .userId(42L)
                .username("teacher")
                .orgUnitId(100L)
                .orgUnitPath("1.10.100.")
                .roleIds(roles == null ? Collections.emptyList()
                        : roles.stream().map(UserContext.ScopedRoleInfo::getRoleId).toList())
                .tenantId(1L)
                .scopedRoles(roles)
                .build();
    }

    private Object build(DataPermission ann, DataModulePO module, UserContext ctx, Long tenantId) {
        return ReflectionTestUtils.invokeMethod(interceptor, "buildScopedCondition",
                ann, module, ctx, tenantId, "READ");
    }

    private String sqlOf(Object cond) {
        return (String) ReflectionTestUtils.getField(cond, "sql");
    }

    @SuppressWarnings("unchecked")
    private List<Object> paramsOf(Object cond) {
        return (List<Object>) ReflectionTestUtils.getField(cond, "params");
    }

    @Test
    @DisplayName("CUSTOM 含 GRADE/部门 org → 必须子树展开 (org + 后代), 而非裸 IN(<id>)")
    void customMembershipSubtreeExpandsGrantedOrg() {
        UserContext ctx = userWithScopedRoles(List.of(scopedRole(6L, RoleAssignmentScope.ALL, 0L, null)));
        when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(6L), anyString(), anyString()))
                .thenReturn(customOrgSpec(new HashSet<>(Set.of(77L))));

        Object cond = build(studentAnnotation(), moduleConfig("student"), ctx, 1L);
        assertThat(cond).isNotNull();

        String sql = sqlOf(cond);
        // Still wrapped in the membership shell.
        assertThat(sql)
                .startsWith("s.user_id IN (")
                .contains("SELECT ar.subject_id FROM access_relations ar")
                .contains("ar.relation = 'member'");

        // The fix: the granted org id must be subtree-expanded against org_units,
        // NOT used as a bare resource_id match.
        assertThat(sql)
                .contains("ar.resource_id IN (")
                .contains("org_units")
                .contains("tree_path LIKE CONCAT(g.tree_path, '%')")
                // the granted id must seed the subtree expansion (g.id IN (...))
                .contains("g.id IN (77)")
                // must NOT be the raw bare match that misses descendant members
                .doesNotContain("ar.resource_id IN (77)");

        // Positional params: [outer ar.tenant_id, inner subquery o.tenant_id].
        assertThat(paramsOf(cond)).hasSize(2);
    }

    @Test
    @DisplayName("CUSTOM 无配置 org → 仍拒绝所有 1 = 0 (空 guard 保留)")
    void customMembershipEmptyOrgsStillDeniesAll() {
        UserContext ctx = userWithScopedRoles(List.of(scopedRole(5L, RoleAssignmentScope.ALL, 0L, null)));
        when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(5L), anyString(), anyString()))
                .thenReturn(customOrgSpec(new HashSet<>()));

        Object cond = build(studentAnnotation(), moduleConfig("student"), ctx, 1L);
        assertThat(cond).isNotNull();
        assertThat(sqlOf(cond)).isEqualTo("1 = 0");
    }
}
