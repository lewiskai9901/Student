package com.school.management.infrastructure.access.policy;

import com.school.management.domain.access.model.Role;
import com.school.management.domain.access.model.UserRole;
import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.domain.access.repository.UserRoleRepository;
import com.school.management.infrastructure.access.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理护栏 —— 把"操作者 + 目标用户"的属性解析好，交给 {@link PolicyEngine}（闸3）判。
 *
 * <p>在 UserApplicationService 的增/改/删/停用/重置密码/授角色路径前调用。规则本身是纯函数，
 * 本类负责"取数"：超管判定、目标管理的组织、操作者管理的组织、授的角色里有没有超管。
 *
 * <p>SUPER_ADMIN / admin 关系码均为系统通用常量；本类只读 access_relations + 角色, 不改任何业务。
 */
@Service
@RequiredArgsConstructor
public class UserManagementGuard {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    private static final String ADMIN_RELATION = "admin";
    private static final String ORG_UNIT = "org_unit";
    private static final String USER = "user";

    private final PolicyEngine policyEngine;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final AccessRelationRepository accessRelationRepository;

    /** 增/改/删/停用/重置密码 前调用：校验操作者能否管理该目标用户。 */
    public void checkManage(Long targetUserId, String action) {
        Long subjectId = UserContextHolder.getUserId();
        if (subjectId == null) return; // 系统/内部调用无上下文，跳过护栏
        policyEngine.requirePermit(buildRequest(subjectId, targetUserId, action, null));
    }

    /** 授角色 / 创建带角色 前调用：含"目标保护 + 防超管提权"。targetUserId 为 null 表示新建用户。 */
    public void checkRoleAssignment(Long targetUserId, Collection<Long> roleIds, String action) {
        Long subjectId = UserContextHolder.getUserId();
        if (subjectId == null) return;
        boolean assignsSuperAdmin = containsSuperAdminRole(roleIds);
        policyEngine.requirePermit(buildRequest(subjectId, targetUserId, action, assignsSuperAdmin));
    }

    // ──────────────── 解析 ────────────────

    private AccessRequest buildRequest(Long subjectId, Long targetUserId, String action, Boolean assignsSuperAdmin) {
        boolean subjectSuper = UserContextHolder.isSuperAdmin();
        Set<Long> subjectManaged = subjectSuper ? Set.of() : managedOrgIds(subjectId);

        Map<String, Object> attrs = new HashMap<>();
        if (targetUserId != null) {
            attrs.put(PolicyAttrs.TARGET_IS_SUPER_ADMIN, isSuperAdminUser(targetUserId));
            attrs.put(PolicyAttrs.TARGET_MANAGED_ORG_IDS, managedOrgIds(targetUserId));
        }
        if (assignsSuperAdmin != null) {
            attrs.put(PolicyAttrs.ASSIGNS_SUPER_ADMIN, assignsSuperAdmin);
        }
        return new AccessRequest(subjectId, subjectSuper, subjectManaged, action, USER, targetUserId, attrs);
    }

    /** 用户持 admin 关系的组织 id 集（"他管理哪些组织"）。 */
    private Set<Long> managedOrgIds(Long userId) {
        List<AccessRelation> rels = accessRelationRepository.findBySubjectAndResourceType(USER, userId, ORG_UNIT);
        return rels.stream()
                .filter(r -> ADMIN_RELATION.equals(r.getRelation()))
                .map(AccessRelation::getResourceId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** 目标用户是否持有超管角色。 */
    private boolean isSuperAdminUser(Long userId) {
        List<Long> roleIds = userRoleRepository.findActiveByUserId(userId).stream()
                .map(UserRole::getRoleId).collect(Collectors.toList());
        return containsSuperAdminRole(roleIds);
    }

    private boolean containsSuperAdminRole(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return false;
        return roleRepository.findByIds(List.copyOf(roleIds)).stream().anyMatch(this::isSuperAdminRole);
    }

    private boolean isSuperAdminRole(Role r) {
        return SUPER_ADMIN_ROLE.equalsIgnoreCase(r.getRoleType())
                || SUPER_ADMIN_ROLE.equalsIgnoreCase(r.getRoleCode());
    }
}
