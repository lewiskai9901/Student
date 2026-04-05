package com.school.management.application.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 触发器条件匹配器
 * 支持: 精确匹配, $in, $ne, $lt, $gt, $lte, $gte
 * null/空/{} = 始终匹配
 */
@Slf4j
public class ConditionMatcher {

    public static boolean matches(String conditionJson, Map<String, Object> context, ObjectMapper objectMapper) {
        if (conditionJson == null || conditionJson.isBlank() || "{}".equals(conditionJson.trim()) || "null".equals(conditionJson.trim())) {
            return true;
        }
        try {
            Map<String, Object> conditions = objectMapper.readValue(conditionJson, new TypeReference<>() {});
            if (conditions == null || conditions.isEmpty()) {
                return true;
            }
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                String field = entry.getKey();
                Object expected = entry.getValue();
                Object actual = context.get(field);

                if (expected instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> operators = (Map<String, Object>) expected;
                    if (!matchOperators(operators, actual)) {
                        return false;
                    }
                } else {
                    // exact match
                    if (!String.valueOf(expected).equals(String.valueOf(actual))) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("条件匹配解析失败: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean matchOperators(Map<String, Object> operators, Object actual) {
        for (Map.Entry<String, Object> op : operators.entrySet()) {
            String operator = op.getKey();
            Object expected = op.getValue();
            switch (operator) {
                case "$in":
                    if (expected instanceof List) {
                        List<Object> list = (List<Object>) expected;
                        // Compare as strings for type-safe matching
                        String actualStr = String.valueOf(actual);
                        boolean found = false;
                        for (Object item : list) {
                            if (String.valueOf(item).equals(actualStr)) {
                                found = true;
                                break;
                            }
                            // Also try numeric comparison
                            if (actual instanceof Number && item instanceof Number) {
                                if (((Number) actual).doubleValue() == ((Number) item).doubleValue()) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) return false;
                    }
                    break;
                case "$ne":
                    if (String.valueOf(expected).equals(String.valueOf(actual))) {
                        return false;
                    }
                    break;
                case "$lt":
                    if (toDouble(actual) >= toDouble(expected)) return false;
                    break;
                case "$gt":
                    if (toDouble(actual) <= toDouble(expected)) return false;
                    break;
                case "$lte":
                    if (toDouble(actual) > toDouble(expected)) return false;
                    break;
                case "$gte":
                    if (toDouble(actual) < toDouble(expected)) return false;
                    break;
                default:
                    log.warn("未知操作符: {}", operator);
                    break;
            }
        }
        return true;
    }

    private static double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
