package com.school.management.infrastructure.access.policy;

import com.school.management.infrastructure.access.policy.rules.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 授权护栏引擎 + 4 条规则 单测（纯函数，不起 Spring / 不查库）。
 */
class PolicyEngineTest {

    private final PolicyEngine engine = new PolicyEngine(List.of(
            new ProtectSuperAdminRule(),
            new ProtectOrgManagerRule(),
            new NoSuperAdminGrantRule(),
            new NoSelfCriticalRule()
    ));

    /** 便捷构造：操作者 subjectId(是否超管, 管理的组织) 对 target 做 action, target 属性 attrs。 */
    private AccessRequest req(long subjectId, boolean subjectSuper, Set<Long> subjectManaged,
                              String action, Long targetId, Map<String, Object> attrs) {
        return new AccessRequest(subjectId, subjectSuper, subjectManaged, action, "user", targetId, attrs);
    }

    // ───────── 引擎合并语义 ─────────

    @Test
    @DisplayName("无规则命中 → 默认放行")
    void defaultPermit() {
        var d = engine.decide(req(2L, false, Set.of(), AuthActions.USER_UPDATE, 3L,
                Map.of(PolicyAttrs.TARGET_IS_SUPER_ADMIN, false, PolicyAttrs.TARGET_MANAGED_ORG_IDS, Set.of())));
        assertThat(d.allowed()).isTrue();
    }

    @Test
    @DisplayName("deny-overrides：任一规则 DENY 即拒")
    void denyOverrides() {
        var d = engine.decide(req(2L, false, Set.of(), AuthActions.USER_UPDATE, 3L,
                Map.of(PolicyAttrs.TARGET_IS_SUPER_ADMIN, true)));
        assertThat(d.allowed()).isFalse();
        assertThat(d.ruleId()).isEqualTo("protect-super-admin");
    }

    @Test
    @DisplayName("requirePermit 拒绝时抛 AccessDeniedException")
    void requirePermitThrows() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                engine.requirePermit(req(2L, false, Set.of(), AuthActions.USER_UPDATE, 3L,
                        Map.of(PolicyAttrs.TARGET_IS_SUPER_ADMIN, true))));
    }

    // ───────── a-admin 核心场景 ─────────

    @Test
    @DisplayName("a(成员)改 admin(超管+组织A管理者) → 拒")
    void memberCannotManageAdmin() {
        var d = engine.decide(req(2L, false, Set.of(), AuthActions.USER_UPDATE, 1L,
                Map.of(PolicyAttrs.TARGET_IS_SUPER_ADMIN, true,
                       PolicyAttrs.TARGET_MANAGED_ORG_IDS, Set.of(100L))));
        assertThat(d.allowed()).isFalse();
    }

    @Test
    @DisplayName("非超管改'管辖外组织管理者' → 拒(protect-org-manager)")
    void cannotManageOutOfScopeManager() {
        // target 管理组织 100；操作者只管理 200 → 不覆盖 100 → 拒
        var d = engine.decide(req(2L, false, Set.of(200L), AuthActions.USER_UPDATE, 5L,
                Map.of(PolicyAttrs.TARGET_IS_SUPER_ADMIN, false,
                       PolicyAttrs.TARGET_MANAGED_ORG_IDS, Set.of(100L))));
        assertThat(d.allowed()).isFalse();
        assertThat(d.ruleId()).isEqualTo("protect-org-manager");
    }

    @Test
    @DisplayName("操作者管辖覆盖目标所管组织 → 放行")
    void canManageWhenCoversManagedOrgs() {
        var d = engine.decide(req(2L, false, Set.of(100L, 200L), AuthActions.USER_UPDATE, 5L,
                Map.of(PolicyAttrs.TARGET_IS_SUPER_ADMIN, false,
                       PolicyAttrs.TARGET_MANAGED_ORG_IDS, Set.of(100L))));
        assertThat(d.allowed()).isTrue();
    }

    @Test
    @DisplayName("管理普通成员(非超管/非管理者) → 放行")
    void canManagePlainMember() {
        var d = engine.decide(req(2L, false, Set.of(100L), AuthActions.USER_UPDATE, 7L,
                Map.of(PolicyAttrs.TARGET_IS_SUPER_ADMIN, false,
                       PolicyAttrs.TARGET_MANAGED_ORG_IDS, Set.of())));
        assertThat(d.allowed()).isTrue();
    }

    // ───────── 超管不受限 ─────────

    @Test
    @DisplayName("超管可操作任何人")
    void superAdminUnrestricted() {
        var d = engine.decide(req(1L, true, Set.of(), AuthActions.USER_DELETE, 9L,
                Map.of(PolicyAttrs.TARGET_IS_SUPER_ADMIN, true,
                       PolicyAttrs.TARGET_MANAGED_ORG_IDS, Set.of(100L))));
        assertThat(d.allowed()).isTrue();
    }

    // ───────── 防提权 ─────────

    @Test
    @DisplayName("非超管授超管角色 → 拒(no-super-admin-grant)")
    void cannotGrantSuperAdmin() {
        var d = engine.decide(req(2L, false, Set.of(), AuthActions.USER_ASSIGN_ROLES, 7L,
                Map.of(PolicyAttrs.TARGET_IS_SUPER_ADMIN, false,
                       PolicyAttrs.TARGET_MANAGED_ORG_IDS, Set.of(),
                       PolicyAttrs.ASSIGNS_SUPER_ADMIN, true)));
        assertThat(d.allowed()).isFalse();
        assertThat(d.ruleId()).isEqualTo("no-super-admin-grant");
    }

    @Test
    @DisplayName("超管授超管角色 → 放行")
    void superAdminCanGrantSuperAdmin() {
        var d = engine.decide(req(1L, true, Set.of(), AuthActions.USER_ASSIGN_ROLES, 7L,
                Map.of(PolicyAttrs.ASSIGNS_SUPER_ADMIN, true)));
        assertThat(d.allowed()).isTrue();
    }

    // ───────── 防自废 ─────────

    @Test
    @DisplayName("不能删自己")
    void cannotDeleteSelf() {
        var d = engine.decide(req(2L, false, Set.of(), AuthActions.USER_DELETE, 2L,
                Map.of(PolicyAttrs.TARGET_IS_SUPER_ADMIN, false, PolicyAttrs.TARGET_MANAGED_ORG_IDS, Set.of())));
        assertThat(d.allowed()).isFalse();
        assertThat(d.ruleId()).isEqualTo("no-self-critical");
    }
}
