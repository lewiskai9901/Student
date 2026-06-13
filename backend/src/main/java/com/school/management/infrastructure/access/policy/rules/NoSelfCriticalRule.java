package com.school.management.infrastructure.access.policy.rules;

import com.school.management.infrastructure.access.policy.*;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

/**
 * 不能对自己执行高危操作（删除 / 停用）—— 防止管理员把自己锁死或误删。
 */
@Component
public class NoSelfCriticalRule implements PolicyRule {

    private static final Set<String> CRITICAL = Set.of(
            AuthActions.USER_DELETE, AuthActions.USER_DISABLE);

    @Override public String id() { return "no-self-critical"; }

    @Override public boolean appliesTo(AccessRequest req) {
        return "user".equals(req.resourceType()) && CRITICAL.contains(req.action());
    }

    @Override public Effect evaluate(AccessRequest req) {
        return Objects.equals(req.subjectUserId(), req.resourceId()) ? Effect.DENY : Effect.NOT_APPLICABLE;
    }

    @Override public String denyReason(AccessRequest req) {
        return "不能对自己执行删除/停用操作";
    }
}
