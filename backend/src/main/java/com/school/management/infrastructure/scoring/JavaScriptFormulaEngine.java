package com.school.management.infrastructure.scoring;

import com.school.management.domain.scoring.service.FormulaEngineService;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavaScript公式引擎实现（基于GraalVM）
 */
@Slf4j
@Service
public class JavaScriptFormulaEngine implements FormulaEngineService {

    // 内置函数定义
    private static final String BUILT_IN_FUNCTIONS = """
        // 数学函数
        function MAX(...args) { return Math.max(...args); }
        function MIN(...args) { return Math.min(...args); }
        function SUM(...args) { return args.reduce((a, b) => a + b, 0); }
        function AVG(...args) { return args.length > 0 ? SUM(...args) / args.length : 0; }
        function ABS(x) { return Math.abs(x); }
        function ROUND(x, decimals) { return Number(Math.round(x + 'e' + (decimals || 0)) + 'e-' + (decimals || 0)); }
        function FLOOR(x) { return Math.floor(x); }
        function CEIL(x) { return Math.ceil(x); }

        // 逻辑函数
        function IF(condition, trueVal, falseVal) { return condition ? trueVal : falseVal; }
        function AND(...args) { return args.every(Boolean); }
        function OR(...args) { return args.some(Boolean); }
        function NOT(x) { return !x; }

        // 统计函数
        function COUNT(arr) { return Array.isArray(arr) ? arr.length : 0; }
        function COUNTIF(arr, condition) {
            if (!Array.isArray(arr)) return 0;
            if (typeof condition === 'function') return arr.filter(condition).length;
            return arr.filter(x => x === condition).length;
        }
        function SUMIF(arr, condition, sumArr) {
            if (!Array.isArray(arr)) return 0;
            sumArr = sumArr || arr;
            return arr.reduce((sum, val, idx) => {
                const match = typeof condition === 'function' ? condition(val) : val === condition;
                return match ? sum + (sumArr[idx] || 0) : sum;
            }, 0);
        }

        // 区间判断函数
        function BETWEEN(value, min, max) { return value >= min && value <= max; }
        function GRADE(value, ranges) {
            if (!Array.isArray(ranges)) return null;
            for (const range of ranges) {
                if (value >= range.min && value <= range.max) {
                    return range.result;
                }
            }
            return null;
        }

        // 时间函数
        function NOW() { return new Date(); }
        function TODAY() {
            const d = new Date();
            d.setHours(0, 0, 0, 0);
            return d;
        }
        function WEEKDAY(date) { return (date || new Date()).getDay(); }
        function HOUR(date) { return (date || new Date()).getHours(); }
        function DATEDIFF(date1, date2, unit) {
            const d1 = new Date(date1);
            const d2 = new Date(date2);
            const diffMs = d2 - d1;
            switch (unit || 'days') {
                case 'days': return Math.floor(diffMs / 86400000);
                case 'hours': return Math.floor(diffMs / 3600000);
                case 'minutes': return Math.floor(diffMs / 60000);
                default: return diffMs;
            }
        }

        // 限制函数
        function CLAMP(value, min, max) { return Math.min(Math.max(value, min), max); }
        function LIMIT(value, max) { return Math.min(value, max); }
        """;

    // 公式缓存
    private final ConcurrentHashMap<String, String> formulaCache = new ConcurrentHashMap<>();

    @Override
    public Double evaluateNumeric(String formula, Map<String, Object> context) {
        Object result = evaluate(formula, context);
        if (result == null) return 0.0;
        if (result instanceof Number) {
            return ((Number) result).doubleValue();
        }
        try {
            return Double.parseDouble(result.toString());
        } catch (NumberFormatException e) {
            log.warn("公式结果无法转换为数字: {}", result);
            return 0.0;
        }
    }

    @Override
    public Boolean evaluateBoolean(String formula, Map<String, Object> context) {
        Object result = evaluate(formula, context);
        if (result == null) return false;
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        if (result instanceof Number) {
            return ((Number) result).doubleValue() != 0;
        }
        return Boolean.parseBoolean(result.toString());
    }

    @Override
    public Object evaluate(String formula, Map<String, Object> context) {
        if (formula == null || formula.trim().isEmpty()) {
            return null;
        }

        try (Context jsContext = Context.newBuilder("js")
                .option("engine.WarnInterpreterOnly", "false")
                .allowAllAccess(false)
                .build()) {

            // 执行内置函数定义
            jsContext.eval("js", BUILT_IN_FUNCTIONS);

            // 绑定上下文变量
            Value bindings = jsContext.getBindings("js");
            if (context != null) {
                for (Map.Entry<String, Object> entry : context.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    // 将变量名转换为合法的JavaScript标识符
                    String jsKey = key.startsWith("$") ? key.substring(1) : key;
                    bindings.putMember(jsKey, value);
                }
            }

            // 预处理公式中的变量引用
            String processedFormula = preprocessFormula(formula);

            // 执行公式
            Value result = jsContext.eval("js", processedFormula);

            // 转换结果
            if (result.isNull()) {
                return null;
            }
            if (result.isBoolean()) {
                return result.asBoolean();
            }
            if (result.isNumber()) {
                if (result.fitsInLong()) {
                    long longVal = result.asLong();
                    if (longVal == (int) longVal) {
                        return (int) longVal;
                    }
                    return longVal;
                }
                return result.asDouble();
            }
            if (result.isString()) {
                return result.asString();
            }
            return result.toString();

        } catch (PolyglotException e) {
            log.error("公式执行失败: {} - {}", formula, e.getMessage());
            throw new RuntimeException("公式执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validate(String formula) {
        if (formula == null || formula.trim().isEmpty()) {
            return false;
        }

        try (Context jsContext = Context.newBuilder("js")
                .option("engine.WarnInterpreterOnly", "false")
                .allowAllAccess(false)
                .build()) {

            // 执行内置函数定义
            jsContext.eval("js", BUILT_IN_FUNCTIONS);

            // 创建虚拟变量
            Value bindings = jsContext.getBindings("js");
            bindings.putMember("input", 0);
            bindings.putMember("value", 0);
            bindings.putMember("score", 100);
            bindings.putMember("result", 0);
            bindings.putMember("baseScore", 100);
            bindings.putMember("totalDeductions", 0);
            bindings.putMember("totalBonus", 0);

            // 尝试解析公式
            String processedFormula = preprocessFormula(formula);
            jsContext.eval("js", "function __validate__() { return " + processedFormula + "; }");
            return true;

        } catch (Exception e) {
            log.debug("公式语法验证失败: {} - {}", formula, e.getMessage());
            return false;
        }
    }

    @Override
    public String getExecutionError(String formula, Map<String, Object> context) {
        try {
            evaluate(formula, context);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * 预处理公式
     * 将 $变量名 转换为 变量名
     */
    private String preprocessFormula(String formula) {
        return formulaCache.computeIfAbsent(formula, f -> f.replaceAll("\\$([a-zA-Z_][a-zA-Z0-9_]*)", "$1"));
    }
}
