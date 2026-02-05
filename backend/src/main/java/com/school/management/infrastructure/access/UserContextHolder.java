package com.school.management.infrastructure.access;

/**
 * 用户上下文持有者 (V5)
 * 使用ThreadLocal存储当前请求的用户上下文
 * 线程安全，每个请求独立
 */
public class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    /**
     * 是否启用数据权限过滤
     * 可通过此标志临时禁用数据权限（如系统任务、定时任务等）
     */
    private static final ThreadLocal<Boolean> DATA_PERMISSION_ENABLED = ThreadLocal.withInitial(() -> true);

    /**
     * 设置当前用户上下文
     */
    public static void setContext(UserContext context) {
        CONTEXT.set(context);
    }

    /**
     * 获取当前用户上下文
     */
    public static UserContext getContext() {
        return CONTEXT.get();
    }

    /**
     * 清除当前用户上下文
     * 应在请求结束时调用，防止内存泄漏
     */
    public static void clear() {
        CONTEXT.remove();
        DATA_PERMISSION_ENABLED.remove();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        UserContext context = getContext();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        UserContext context = getContext();
        return context != null ? context.getUsername() : null;
    }

    /**
     * 获取当前用户所属组织单元ID
     */
    public static Long getOrgUnitId() {
        UserContext context = getContext();
        return context != null ? context.getOrgUnitId() : null;
    }

    /**
     * 判断当前用户是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        UserContext context = getContext();
        return context != null && context.isSuperAdmin();
    }

    /**
     * 启用数据权限过滤
     */
    public static void enableDataPermission() {
        DATA_PERMISSION_ENABLED.set(true);
    }

    /**
     * 禁用数据权限过滤
     */
    public static void disableDataPermission() {
        DATA_PERMISSION_ENABLED.set(false);
    }

    /**
     * 判断数据权限过滤是否启用
     */
    public static boolean isDataPermissionEnabled() {
        return Boolean.TRUE.equals(DATA_PERMISSION_ENABLED.get());
    }

    /**
     * 在禁用数据权限的情况下执行操作
     */
    public static <T> T executeWithoutDataPermission(java.util.function.Supplier<T> supplier) {
        boolean wasEnabled = isDataPermissionEnabled();
        try {
            disableDataPermission();
            return supplier.get();
        } finally {
            if (wasEnabled) {
                enableDataPermission();
            }
        }
    }

    /**
     * 在禁用数据权限的情况下执行操作（无返回值）
     */
    public static void runWithoutDataPermission(Runnable runnable) {
        boolean wasEnabled = isDataPermissionEnabled();
        try {
            disableDataPermission();
            runnable.run();
        } finally {
            if (wasEnabled) {
                enableDataPermission();
            }
        }
    }
}
