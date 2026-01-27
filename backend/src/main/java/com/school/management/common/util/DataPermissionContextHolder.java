package com.school.management.common.util;

/**
 * 数据权限上下文持有者
 * 使用ThreadLocal存储当前线程的数据权限SQL条件
 */
public class DataPermissionContextHolder {

    private static final ThreadLocal<Boolean> ENABLED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<String> DATA_SCOPE_SQL = new ThreadLocal<>();

    public static void enable() {
        ENABLED.set(true);
    }

    public static void disable() {
        ENABLED.set(false);
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    public static void setDataScopeSql(String sql) {
        DATA_SCOPE_SQL.set(sql);
    }

    public static String getDataScopeSql() {
        return DATA_SCOPE_SQL.get();
    }

    public static void clear() {
        ENABLED.remove();
        DATA_SCOPE_SQL.remove();
    }
}
