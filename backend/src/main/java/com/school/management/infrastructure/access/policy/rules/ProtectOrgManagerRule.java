package com.school.management.infrastructure.access.policy.rules;

import com.school.management.infrastructure.access.policy.*;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 成员不能管理"本组织（或自己管不到的组织）的管理者"。
 *
 * <p>判据（不依赖 role.level，纯结构关系）：目标是某组织的管理者(admin 关系)，而操作者并不
 * 管理那个组织 → 拒。即"目标管理的组织集 ⊄ 操作者管理的组织集"则拒。
 *
 * <p>例：admin 管理 {A}，a(成员)管理 {} → {A} 不被 {} 包含 → a 不能动 admin。
 */
@Component
public class ProtectOrgManagerRule implements PolicyRule {

    private static final Set<String> GUARDED = Set.of(
            AuthActions.USER_UPDATE, AuthActions.USER_DELETE, AuthActions.USER_DISABLE,
            AuthActions.USER_RESET_PASSWORD, AuthActions.USER_ASSIGN_ROLES);

    @Override public String id() { return "protect-org-manager"; }

    @Override public boolean appliesTo(AccessRequest req) {
        return "user".equals(req.resourceType()) && GUARDED.contains(req.action());
    }

    @Override public Effect evaluate(AccessRequest req) {
        if (req.subjectIsSuperAdmin()) return Effect.NOT_APPLICABLE;
        Set<Long> targetManaged = req.attrLongSet(PolicyAttrs.TARGET_MANAGED_ORG_IDS);
        if (targetManaged.isEmpty()) return Effect.NOT_APPLICABLE;        // 目标不是任何组织的管理者
        // 目标管理的组织里，存在操作者管不到的 → 拒
        boolean subjectCoversAll = req.subjectManagedOrgIds().containsAll(targetManaged);
        return subjectCoversAll ? Effect.NOT_APPLICABLE : Effect.DENY;
    }

    @Override public String denyReason(AccessRequest req) {
        return "无权操作该用户：对方是你管辖范围外组织的管理者";
    }
}
