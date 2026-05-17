package com.school.management.infrastructure.inspection;

import com.school.management.domain.access.model.ScopeType;
import com.school.management.infrastructure.access.UserContext;
import com.school.management.infrastructure.access.UserContextHolder;
import com.school.management.infrastructure.tenant.TenantContext;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * InspectionScopeHelper 单元测试 (I7).
 *
 * <p>验证 JdbcTemplate 旁路的数据权限收窄逻辑:
 * <ul>
 *   <li>super admin → null (不受限)</li>
 *   <li>data permission 关闭 → null</li>
 *   <li>ALL scope 角色 → null</li>
 *   <li>ORG_UNIT scope 角色 → 子树 id 集合</li>
 *   <li>无 scope 兜底 → primary org 子树</li>
 *   <li>无 scope 无 primary org → 空 set (deny)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InspectionScopeHelper — JdbcTemplate 旁路数据权限收窄")
class InspectionScopeHelperTest {

    @Mock JdbcTemplate jdbcTemplate;
    @InjectMocks InspectionScopeHelper scopeHelper;

    @BeforeEach
    void setUp() {
        UserContextHolder.enableDataPermission();
        TenantContextHolder.setContext(new TenantContext(1L, "default"));
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("不受限路径 — allowedOrgIds() 应返回 null")
    class UnboundedPaths {

        @Test
        @DisplayName("super admin → 不受限")
        void superAdmin_isUnbounded() {
            UserContext ctx = UserContext.builder()
                    .userId(1L).username("admin").superAdmin(true)
                    .build();
            UserContextHolder.setContext(ctx);

            assertThat(scopeHelper.allowedOrgIds()).isNull();
            assertThat(scopeHelper.isUnbounded()).isTrue();
            assertThat(scopeHelper.orgScopeClause("org_unit_id")).isEmpty();
        }

        @Test
        @DisplayName("data permission 关闭 → 不受限")
        void dataPermissionDisabled_isUnbounded() {
            UserContextHolder.disableDataPermission();
            UserContext ctx = UserContext.builder()
                    .userId(2L).username("user").superAdmin(false)
                    .build();
            UserContextHolder.setContext(ctx);

            assertThat(scopeHelper.allowedOrgIds()).isNull();
            assertThat(scopeHelper.orgScopeClause("org_unit_id")).isEmpty();
        }

        @Test
        @DisplayName("任一 ALL scope 角色 → 不受限")
        void anyAllScopeRole_isUnbounded() {
            UserContext ctx = UserContext.builder()
                    .userId(3L).username("user")
                    .scopedRoles(List.of(
                            UserContext.ScopedRoleInfo.builder()
                                    .roleId(10L).scopeType(ScopeType.ORG_UNIT)
                                    .scopeId(5L).scopeOrgPath("/1/5/")
                                    .build(),
                            UserContext.ScopedRoleInfo.builder()
                                    .roleId(11L).scopeType(ScopeType.ALL)
                                    .scopeId(0L)
                                    .build()))
                    .build();
            UserContextHolder.setContext(ctx);

            assertThat(scopeHelper.allowedOrgIds()).isNull();
        }
    }

    @Nested
    @DisplayName("受限路径 — allowedOrgIds() 应返回具体 id 集合")
    class BoundedPaths {

        @Test
        @DisplayName("单 ORG_UNIT scope 角色 → 返回子树 id 集合")
        void singleOrgUnitScope_returnsSubtreeIds() {
            UserContext ctx = UserContext.builder()
                    .userId(4L).username("teacher")
                    .scopedRoles(List.of(
                            UserContext.ScopedRoleInfo.builder()
                                    .roleId(20L).scopeType(ScopeType.ORG_UNIT)
                                    .scopeId(5L).scopeOrgPath("/1/5/")
                                    .build()))
                    .build();
            UserContextHolder.setContext(ctx);
            when(jdbcTemplate.queryForList(
                    eq("SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0"),
                    eq(Long.class), eq(1L), eq("/1/5/%")))
                    .thenReturn(List.of(5L, 6L, 7L));

            Set<Long> ids = scopeHelper.allowedOrgIds();
            assertThat(ids).containsExactlyInAnyOrder(5L, 6L, 7L);
            assertThat(scopeHelper.isUnbounded()).isFalse();

            String clause = scopeHelper.orgScopeClause("t.org_unit_id");
            assertThat(clause).startsWith(" AND t.org_unit_id IN (").endsWith(")");
            assertThat(clause).contains("5", "6", "7");
        }

        @Test
        @DisplayName("无 scoped roles + 有 primary org → 走 primary org 子树兜底")
        void noScopedRoles_fallbackToPrimaryOrg() {
            UserContext ctx = UserContext.builder()
                    .userId(5L).username("user")
                    .orgUnitId(9L).orgUnitPath("/1/9/")
                    .scopedRoles(List.of())
                    .build();
            UserContextHolder.setContext(ctx);
            when(jdbcTemplate.queryForList(
                    any(String.class), eq(Long.class), eq(1L), eq("/1/9/%")))
                    .thenReturn(List.of(9L, 10L));

            assertThat(scopeHelper.allowedOrgIds()).containsExactlyInAnyOrder(9L, 10L);
        }

        @Test
        @DisplayName("无 scoped roles + 无 primary org → 拒绝所有")
        void noScopedRolesNoOrg_denyAll() {
            UserContext ctx = UserContext.builder()
                    .userId(6L).username("orphan")
                    .scopedRoles(List.of())
                    .build();
            UserContextHolder.setContext(ctx);

            assertThat(scopeHelper.allowedOrgIds()).isEmpty();
            assertThat(scopeHelper.orgScopeClause("org_unit_id")).isEqualTo(" AND 1=0");
        }

        @Test
        @DisplayName("多个 ORG_UNIT scope 角色 → union")
        void multipleOrgUnitScopes_union() {
            UserContext ctx = UserContext.builder()
                    .userId(7L).username("multi-role")
                    .scopedRoles(List.of(
                            UserContext.ScopedRoleInfo.builder()
                                    .roleId(30L).scopeType(ScopeType.ORG_UNIT)
                                    .scopeId(5L).scopeOrgPath("/1/5/")
                                    .build(),
                            UserContext.ScopedRoleInfo.builder()
                                    .roleId(31L).scopeType(ScopeType.ORG_UNIT)
                                    .scopeId(8L).scopeOrgPath("/1/8/")
                                    .build()))
                    .build();
            UserContextHolder.setContext(ctx);
            when(jdbcTemplate.queryForList(
                    any(String.class), eq(Long.class), eq(1L), eq("/1/5/%")))
                    .thenReturn(List.of(5L, 6L));
            when(jdbcTemplate.queryForList(
                    any(String.class), eq(Long.class), eq(1L), eq("/1/8/%")))
                    .thenReturn(List.of(8L, 9L));

            assertThat(scopeHelper.allowedOrgIds()).containsExactlyInAnyOrder(5L, 6L, 8L, 9L);
        }
    }

    @Nested
    @DisplayName("orgScopeClause SQL 拼接")
    class ClauseAssembly {

        @Test
        @DisplayName("不受限 → 空字符串 (调用方直接拼无影响)")
        void unbounded_emptyClause() {
            UserContext ctx = UserContext.builder()
                    .userId(1L).superAdmin(true).build();
            UserContextHolder.setContext(ctx);

            assertThat(scopeHelper.orgScopeClause("anything")).isEmpty();
        }

        @Test
        @DisplayName("拒绝 → ' AND 1=0' 让 SQL 0 行返回")
        void deny_andOneEqualsZero() {
            UserContext ctx = UserContext.builder()
                    .userId(1L).scopedRoles(List.of()).build();
            UserContextHolder.setContext(ctx);

            assertThat(scopeHelper.orgScopeClause("col")).isEqualTo(" AND 1=0");
        }
    }
}
