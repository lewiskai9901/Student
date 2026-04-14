package com.school.management.domain.access.model;

/**
 * Permission scope — partitions permissions into three usage contexts.
 *
 * <p>See docs/security/access-control-guide.md
 */
public enum PermissionScope {

    /**
     * Any authenticated user. No role check.
     * Example: 查看首页公告、公开查询接口。
     */
    PUBLIC,

    /**
     * Personal space only (/my/*). User operates on their own data.
     * Casbin interceptor enforces resource ownership via Layer 2 SELF scope.
     * Example: my:schedule:view, my:profile:edit.
     */
    SELF,

    /**
     * Management backoffice (/admin/*, /dashboard/*).
     * Caller must hold at least one role that grants a MANAGEMENT permission.
     * Example: system:user:view, inspection:template:edit.
     */
    MANAGEMENT;

    public static PermissionScope fromCode(String code) {
        if (code == null || code.isBlank()) {
            return MANAGEMENT;
        }
        try {
            return PermissionScope.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MANAGEMENT;
        }
    }
}
