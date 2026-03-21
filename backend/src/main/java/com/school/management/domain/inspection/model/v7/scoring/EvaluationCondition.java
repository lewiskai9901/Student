package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.ValueObject;

import java.util.Map;

/**
 * 评选条件值对象 — 从 EvaluationLevel.conditions JSON 反序列化
 *
 * conditionType 枚举说明:
 *   SCORE_AVG       — 平均分 operator threshold
 *   SCORE_MIN       — 最低单次分数
 *   SCORE_MAX       — 最高单次分数
 *   SCORE_EVERY     — 每次分数都满足条件
 *   SCORE_COUNT     — 达标次数 >= threshold
 *   SCORE_SECTION_ZERO — 某分区得零分次数 <= threshold
 *   EVENT_COUNT     — 特定事件发生次数
 *   EVENT_ROLE      — 特定角色参与事件
 *   EVENT_SCORE_SUM — 事件累计分数
 *   PREV_EVAL       — 上期评选结果
 *   RANK_PERCENTILE — 排名百分位
 *   TREND_IMPROVE   — 趋势改善幅度
 */
public class EvaluationCondition implements ValueObject {

    private final String conditionType;
    private final String operator;        // GTE / LTE / GT / LT / EQ / NEQ
    private final String threshold;
    private final Map<String, Object> parameters;
    private final String description;

    public EvaluationCondition(String conditionType, String operator, String threshold,
                                Map<String, Object> parameters, String description) {
        this.conditionType = conditionType;
        this.operator = operator;
        this.threshold = threshold;
        this.parameters = parameters;
        this.description = description;
    }

    public String getConditionType() { return conditionType; }
    public String getOperator() { return operator; }
    public String getThreshold() { return threshold; }
    public Map<String, Object> getParameters() { return parameters; }
    public String getDescription() { return description; }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EvaluationCondition)) return false;
        EvaluationCondition o = (EvaluationCondition) other;
        return java.util.Objects.equals(conditionType, o.conditionType)
                && java.util.Objects.equals(operator, o.operator)
                && java.util.Objects.equals(threshold, o.threshold);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(conditionType, operator, threshold);
    }
}
