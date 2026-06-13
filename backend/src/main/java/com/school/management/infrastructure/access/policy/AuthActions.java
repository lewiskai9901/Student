package com.school.management.infrastructure.access.policy;

/**
 * 授权动作常量 — 策略引擎按 action 字符串匹配规则。
 *
 * <p>命名沿用权限码风格 {@code 资源:动作}，但这是"动作语义标识"，与 Casbin 功能权限码独立
 * （功能权限管"能不能调"，这里管"对这个目标能不能下手"）。
 */
public final class AuthActions {
    private AuthActions() {}

    public static final String USER_CREATE        = "user:create";
    public static final String USER_UPDATE        = "user:update";
    public static final String USER_DELETE        = "user:delete";
    public static final String USER_DISABLE       = "user:disable";
    public static final String USER_RESET_PASSWORD = "user:reset-password";
    public static final String USER_ASSIGN_ROLES  = "user:role:assign";
}
