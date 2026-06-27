package com.school.management.domain.access.model;

/**
 * 角色授予作用域常量 (user_roles.scope_type 的值): 全局 / 限定某组织。
 * 注: 这是"角色授予作用域"这条轴, 与"数据范围"(ScopePreset/OrgAnchor) 和"权限可见性"(PermissionScope)
 * 是三条不同的轴 —— 重命名自旧 ScopeType 以消除与 inspection.ScopeType 的字面撞名。
 */
public final class RoleAssignmentScope {

    /** Global scope - no restriction */
    public static final String ALL = "ALL";

    /** Scoped to a specific org unit and its children */
    public static final String ORG_UNIT = "ORG_UNIT";

    private RoleAssignmentScope() {}

    public static boolean isValid(String scopeType) {
        return ALL.equals(scopeType) || ORG_UNIT.equals(scopeType);
    }
}
