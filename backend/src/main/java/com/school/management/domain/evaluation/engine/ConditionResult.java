package com.school.management.domain.evaluation.engine;

/**
 * 单个条件的评估结果
 */
public record ConditionResult(boolean passed, String actualValue, String description) {

    public static ConditionResult pass(String actualValue, String description) {
        return new ConditionResult(true, actualValue, description);
    }

    public static ConditionResult fail(String actualValue, String description) {
        return new ConditionResult(false, actualValue, description);
    }

    public static ConditionResult error(String reason) {
        return new ConditionResult(false, "ERROR", reason);
    }
}
