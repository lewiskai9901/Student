package com.school.management.application.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.DataBindingPropertyAccessor;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import java.util.List;
import java.util.Map;

/**
 * 触发器条件匹配器
 * 支持:
 *   - 精确匹配
 *   - 操作符: $in, $ne, $lt, $gt, $lte, $gte
 *   - SpEL 分支: {"spel": "#event.score < 60 and #event.status != 'APPROVED'"}
 * null/空/{} = 始终匹配
 *
 * M4.1: SpEL 沙箱化
 *   使用 SimpleEvaluationContext.forReadOnlyDataBinding() 构造上下文,
 *   禁止 T() 类型引用 / 方法调用任意 Java 类 (CVE-2022-22963 类攻击防护).
 *   #event 变量暴露完整 context map, 支持嵌套字段读取 (e.g. #event.student.name).
 *   解析异常返回 false, 不抛给调用方 (保持"不影响业务"语义).
 */
@Slf4j
public class ConditionMatcher {

    /** SpEL 解析器无状态, 线程安全, static 复用. */
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    public static boolean matches(String conditionJson, Map<String, Object> context, ObjectMapper objectMapper) {
        if (conditionJson == null || conditionJson.isBlank() || "{}".equals(conditionJson.trim()) || "null".equals(conditionJson.trim())) {
            return true;
        }
        try {
            Map<String, Object> conditions = objectMapper.readValue(conditionJson, new TypeReference<>() {});
            if (conditions == null || conditions.isEmpty()) {
                return true;
            }

            // SpEL 分支 — 沙箱化 (只读数据绑定, 禁止任意代码执行)
            if (conditions.containsKey("spel")) {
                Object spelObj = conditions.get("spel");
                if (spelObj == null) return true;
                String spel = String.valueOf(spelObj);
                if (spel.isBlank()) return true;
                return evaluateSpel(spel, context);
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

    /**
     * 评估 SpEL 表达式.
     * SimpleEvaluationContext.forReadOnlyDataBinding() 默认:
     *   - 禁止 T() 类型引用 (block Runtime/ProcessBuilder etc.)
     *   - 禁止构造器调用 (block new ...)
     *   - 允许属性读取 / Map[] / List[]
     *
     * @param spel    表达式字符串, #event 暴露 context map
     * @param context 触发器 context, 作为 #event 变量
     * @return 布尔结果; 表达式异常/非布尔值 → false
     */
    private static boolean evaluateSpel(String spel, Map<String, Object> context) {
        try {
            // forPropertyAccessors(MapAccessor + ReadOnlyDataBindingPropertyAccessor):
            //   - MapAccessor 让 #event.key 等价于 #event['key'] (符合业务直觉)
            //   - DataBindingPropertyAccessor.forReadOnlyAccess 支持 POJO getter 但禁写
            //   - SimpleEvaluationContext 本身还会:
            //       ✗ 禁止 T() 类型引用 (block Runtime/ProcessBuilder)
            //       ✗ 禁止构造器调用 (block new ...)
            //       ✗ 只允许 instance method (若调 .withInstanceMethods)
            SimpleEvaluationContext evalCtx = SimpleEvaluationContext
                .forPropertyAccessors(
                    new MapAccessor(),
                    DataBindingPropertyAccessor.forReadOnlyAccess())
                .build();
            evalCtx.setVariable("event", context);
            Expression expr = SPEL_PARSER.parseExpression(spel);
            Boolean result = expr.getValue(evalCtx, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("[ConditionMatcher] SpEL 表达式执行失败: {} ({})", spel, e.getMessage());
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
