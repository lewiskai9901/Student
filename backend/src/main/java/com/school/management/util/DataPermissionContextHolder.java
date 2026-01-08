package com.school.management.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据权限上下文持有者
 * 使用 ThreadLocal 存储当前请求的数据权限过滤条件
 */
public class DataPermissionContextHolder {

    private static final ThreadLocal<Map<String, Object>> CONTEXT = ThreadLocal.withInitial(HashMap::new);

    /**
     * 设置数据权限SQL片段
     */
    public static void setDataScopeSql(String sql) {
        CONTEXT.get().put("dataScopeSql", sql);
    }

    /**
     * 获取数据权限SQL片段
     */
    public static String getDataScopeSql() {
        return (String) CONTEXT.get().get("dataScopeSql");
    }

    /**
     * 设置是否启用数据权限
     */
    public static void setEnabled(boolean enabled) {
        CONTEXT.get().put("enabled", enabled);
    }

    /**
     * 是否启用数据权限
     */
    public static boolean isEnabled() {
        Boolean enabled = (Boolean) CONTEXT.get().get("enabled");
        return enabled != null && enabled;
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
