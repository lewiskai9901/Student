package com.school.management.infrastructure.access.policy.rules;

import com.school.management.infrastructure.access.policy.*;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 谁都不能操作超管 —— 除非自己也是超管。
 *
 * <p>不依赖 role.level，靠"目标是否超管"这个明确信号，是 a-admin 场景的最强一张网
 * （admin 通常就是超管）。
 */
@Component
public class ProtectSuperAdminRule implements PolicyRule {

    private static final Set<String> GUARDED = Set.of(
            AuthActions.USER_UPDATE, AuthActions.USER_DELETE, AuthActions.USER_DISABLE,
            AuthActions.USER_RESET_PASSWORD, AuthActions.USER_ASSIGN_ROLES);

    @Override public String id() { return "protect-super-admin"; }

    @Override public boolean appliesTo(AccessRequest req) {
        return "user".equals(req.resourceType()) && GUARDED.contains(req.action());
    }

    @Override public Effect evaluate(AccessRequest req) {
        if (req.subjectIsSuperAdmin()) return Effect.NOT_APPLICABLE;       // 超管不受限
        return req.attrBool(PolicyAttrs.TARGET_IS_SUPER_ADMIN) ? Effect.DENY : Effect.NOT_APPLICABLE;
    }

    @Override public String denyReason(AccessRequest req) {
        return "无权操作超级管理员账号";
    }
}
