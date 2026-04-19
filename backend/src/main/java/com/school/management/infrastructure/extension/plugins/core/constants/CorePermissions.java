package com.school.management.infrastructure.extension.plugins.core.constants;

/**
 * 通用核心权限常量 — 与 CorePermissionProvider 对齐.
 *
 * 业务代码通过常量引用权限码:
 * <pre>
 *   {@code @PreAuthorize("hasAuthority('" + CorePermissions.SYSTEM_USER_VIEW + "')") }
 *   {@code @CasbinAccess(CorePermissions.SYSTEM_USER_VIEW) }
 * </pre>
 */
public final class CorePermissions {
    private CorePermissions() {}

    // ─── system:admin ───
    public static final String SYSTEM_ADMIN = "system:admin";

    // ─── system:user ───
    public static final String SYSTEM_USER_VIEW   = "system:user:view";
    public static final String SYSTEM_USER_ADD    = "system:user:add";
    public static final String SYSTEM_USER_EDIT   = "system:user:edit";
    public static final String SYSTEM_USER_DELETE = "system:user:delete";

    // ─── system:role ───
    public static final String SYSTEM_ROLE_VIEW   = "system:role:view";
    public static final String SYSTEM_ROLE_ADD    = "system:role:add";
    public static final String SYSTEM_ROLE_EDIT   = "system:role:edit";
    public static final String SYSTEM_ROLE_DELETE = "system:role:delete";

    // ─── system:permission ───
    public static final String SYSTEM_PERMISSION_VIEW   = "system:permission:view";
    public static final String SYSTEM_PERMISSION_ADD    = "system:permission:add";
    public static final String SYSTEM_PERMISSION_EDIT   = "system:permission:edit";
    public static final String SYSTEM_PERMISSION_DELETE = "system:permission:delete";

    // ─── system:config ───
    public static final String SYSTEM_CONFIG_VIEW   = "system:config:view";
    public static final String SYSTEM_CONFIG_ADD    = "system:config:add";
    public static final String SYSTEM_CONFIG_EDIT   = "system:config:edit";
    public static final String SYSTEM_CONFIG_DELETE = "system:config:delete";

    // ─── system:announcement ───
    public static final String SYSTEM_ANNOUNCEMENT_VIEW   = "system:announcement:view";
    public static final String SYSTEM_ANNOUNCEMENT_ADD    = "system:announcement:add";
    public static final String SYSTEM_ANNOUNCEMENT_EDIT   = "system:announcement:edit";
    public static final String SYSTEM_ANNOUNCEMENT_DELETE = "system:announcement:delete";

    // ─── system:operlog ───
    public static final String SYSTEM_OPERLOG_VIEW   = "system:operlog:view";
    public static final String SYSTEM_OPERLOG_DELETE = "system:operlog:delete";
    public static final String SYSTEM_OPERLOG_CLEAR  = "system:operlog:clear";

    // ─── task ───
    public static final String TASK_CREATE  = "task:create";
    public static final String TASK_EXECUTE = "task:execute";
    public static final String TASK_APPROVE = "task:approve";
    public static final String TASK_MANAGE  = "task:manage";

    // ─── workflow ───
    public static final String WORKFLOW_VIEW   = "workflow:view";
    public static final String WORKFLOW_CREATE = "workflow:create";
    public static final String WORKFLOW_UPDATE = "workflow:update";
    public static final String WORKFLOW_DELETE = "workflow:delete";
    public static final String WORKFLOW_DEPLOY = "workflow:deploy";

    // ─── wechat ───
    public static final String WECHAT_PUSH_VIEW = "wechat:push:view";
    public static final String WECHAT_PUSH_SEND = "wechat:push:send";
}
