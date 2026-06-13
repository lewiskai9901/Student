package com.school.management.infrastructure.access.policy.rules;

import com.school.management.infrastructure.access.policy.*;
import org.springframework.stereotype.Component;

/**
 * 防提权：非超管不能把"超管角色"授予任何人（含自己、含创建用户时附带）。
 *
 * <p>挡住最危险的一种越权——把自己或他人升成超管。不依赖 role.level。
 */
@Component
public class NoSuperAdminGrantRule implements PolicyRule {

    @Override public String id() { return "no-super-admin-grant"; }

    @Override public boolean appliesTo(AccessRequest req) {
        return "user".equals(req.resourceType())
                && (AuthActions.USER_ASSIGN_ROLES.equals(req.action())
                    || AuthActions.USER_CREATE.equals(req.action()));
    }

    @Override public Effect evaluate(AccessRequest req) {
        if (req.subjectIsSuperAdmin()) return Effect.NOT_APPLICABLE;
        return req.attrBool(PolicyAttrs.ASSIGNS_SUPER_ADMIN) ? Effect.DENY : Effect.NOT_APPLICABLE;
    }

    @Override public String denyReason(AccessRequest req) {
        return "无权授予超级管理员角色";
    }
}
