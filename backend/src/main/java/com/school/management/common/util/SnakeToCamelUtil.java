package com.school.management.common.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility to convert snake_case Map keys to camelCase.
 * Used when JdbcTemplate returns database column names (snake_case)
 * but the frontend expects camelCase JSON keys.
 */
public final class SnakeToCamelUtil {

    private SnakeToCamelUtil() {}

    /**
     * Convert a single row's keys from snake_case to camelCase.
     */
    public static Map<String, Object> toCamelCase(Map<String, Object> row) {
        if (row == null) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            result.put(snakeToCamel(entry.getKey()), entry.getValue());
        }
        return result;
    }

    /**
     * Convert a list of rows' keys from snake_case to camelCase.
     */
    public static List<Map<String, Object>> toCamelCaseList(List<Map<String, Object>> rows) {
        if (rows == null) return null;
        return rows.stream().map(SnakeToCamelUtil::toCamelCase).collect(Collectors.toList());
    }

    private static String snakeToCamel(String key) {
        if (key == null || key.isEmpty()) return key;
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : key.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                sb.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return sb.toString();
    }
}
