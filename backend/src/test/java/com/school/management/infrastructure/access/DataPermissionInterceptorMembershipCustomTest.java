package com.school.management.infrastructure.access;

import com.school.management.application.access.DynamicModuleService;
import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.ScopeType;
import com.school.management.domain.access.model.valueobject.MergedDataScope;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Focused tests for the CUSTOM branch of {@code buildMembershipCondition}.
 *
 * <p>Mirrors the test infrastructure of {@link DataPermissionInterceptorTest}
 * ({@code BuildMembershipCondition} nested class): the interceptor is constructed
 * with reflectively-injected mocks, {@code getMergedScope} is stubbed, and
 * {@code buildScopedCondition} is invoked via reflection to drive the private
 * {@code buildMembershipCondition} indirectly (matching the sibling test's
 * indirection — no visibility change).
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
        ReflectionTestUtils.setField(interceptor, "pluginDataScopeRouter", pluginDataScopeRouter);
        UserContextHolder.clear();
        UserContextHolder.enableDataPermission();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    // ── fixtures (copied from sibling test to stay isolated) ──

    @DataPermission(module = "student", tableAlias = "s",
            viaMembership = true, membershipSubjectColumn = "user_id")
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
        po.setOrgUnitField("org_unit_id");
        po.setCreatorField("created_by");
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
                ann, module, ctx, tenantId);
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
        UserContext ctx = userWithScopedRoles(List.of(scopedRole(6L, ScopeType.ALL, 0L, null)));
        when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(6L), anyString()))
                .thenReturn(DataScope.CUSTOM.getCode());
        MergedDataScope merged = MergedDataScope.builder()
                .moduleCode("student").effectiveScope(DataScope.CUSTOM).build();
        merged.getMergedScopeItems().put("ORG_UNIT", new HashSet<>(Set.of(77L)));
        when(dataPermissionPolicyService.getMergedScope(eq(1L), eq(Collections.singletonList(6L)), anyString()))
                .thenReturn(merged);

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
        UserContext ctx = userWithScopedRoles(List.of(scopedRole(5L, ScopeType.ALL, 0L, null)));
        when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(5L), anyString()))
                .thenReturn(DataScope.CUSTOM.getCode());
        MergedDataScope merged = MergedDataScope.builder()
                .moduleCode("student").effectiveScope(DataScope.CUSTOM).build();
        when(dataPermissionPolicyService.getMergedScope(eq(1L), eq(Collections.singletonList(5L)), anyString()))
                .thenReturn(merged);

        Object cond = build(studentAnnotation(), moduleConfig("student"), ctx, 1L);
        assertThat(cond).isNotNull();
        assertThat(sqlOf(cond)).isEqualTo("1 = 0");
    }
}
