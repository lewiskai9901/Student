package com.school.management.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M4.1: ConditionMatcher SpEL 分支单元测试.
 *
 * 核心关注点:
 *   1. SpEL 分支能解析 #event.xxx 并返回 Boolean
 *   2. 嵌套 Map 字段也能访问
 *   3. 非法表达式返回 false (不抛)
 *   4. 沙箱阻断 T(java.lang.Runtime) 类攻击
 */
class ConditionMatcherSpelTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void simpleSpelGreaterThan() {
        String rule = "{\"spel\": \"#event.score < 60\"}";

        Map<String, Object> ctx1 = new HashMap<>();
        ctx1.put("score", 50);
        assertTrue(ConditionMatcher.matches(rule, ctx1, om),
            "score=50 应满足 < 60");

        Map<String, Object> ctx2 = new HashMap<>();
        ctx2.put("score", 70);
        assertFalse(ConditionMatcher.matches(rule, ctx2, om),
            "score=70 不应满足 < 60");
    }

    @Test
    void spelCanAccessNestedFields() {
        String rule = "{\"spel\": \"#event.student.name == 'Tom'\"}";

        Map<String, Object> student = new HashMap<>();
        student.put("id", 1);
        student.put("name", "Tom");
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("student", student);

        assertTrue(ConditionMatcher.matches(rule, ctx, om),
            "嵌套字段 #event.student.name == 'Tom' 应匹配");

        student.put("name", "Jerry");
        assertFalse(ConditionMatcher.matches(rule, ctx, om),
            "嵌套字段 name=Jerry 不应匹配");
    }

    @Test
    void spelComplexBooleanExpression() {
        String rule = "{\"spel\": \"#event.score < 60 and #event.status != 'APPROVED'\"}";

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("score", 50);
        ctx.put("status", "PENDING");
        assertTrue(ConditionMatcher.matches(rule, ctx, om));

        ctx.put("status", "APPROVED");
        assertFalse(ConditionMatcher.matches(rule, ctx, om));
    }

    @Test
    void invalidSpelReturnsFalseAndDoesNotThrow() {
        String rule = "{\"spel\": \"##invalid syntax##\"}";
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("score", 50);
        // 不应抛异常, 应记 WARN 返回 false
        assertFalse(ConditionMatcher.matches(rule, ctx, om),
            "非法 SpEL 应返回 false 不抛");
    }

    @Test
    void spelSandboxBlocksArbitraryJava() {
        // SimpleEvaluationContext.forReadOnlyDataBinding() 不允许 T() 类型引用,
        // 应抛 SpelEvaluationException, 被 catch 后返回 false.
        String rule = "{\"spel\": \"T(java.lang.Runtime).getRuntime() != null\"}";
        Map<String, Object> ctx = new HashMap<>();

        assertFalse(ConditionMatcher.matches(rule, ctx, om),
            "SpEL 沙箱必须阻断 T(java.lang.Runtime) 类引用, 不能返回 true");
    }

    @Test
    void spelEmptyStringTreatedAsAlwaysMatch() {
        // {"spel": ""} 视为总是匹配 (空表达式不计算)
        String rule = "{\"spel\": \"\"}";
        Map<String, Object> ctx = new HashMap<>();
        assertTrue(ConditionMatcher.matches(rule, ctx, om),
            "空 SpEL 表达式应总是匹配");
    }

    @Test
    void spelNonBooleanResultTreatedAsFalse() {
        // SpEL 返回非布尔值 (null / 字符串) 应视为 false (保守, 避免误触发)
        String rule = "{\"spel\": \"#event.missingField\"}";
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("other", 1);
        assertFalse(ConditionMatcher.matches(rule, ctx, om),
            "引用不存在字段返回 null, 应当作 false 处理");
    }
}
