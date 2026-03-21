package com.school.management.domain.evaluation.engine;

import com.school.management.domain.evaluation.model.EvalCondition;

import java.time.LocalDate;

/**
 * 条件评估器接口（适配器模式）
 */
public interface ConditionEvaluator {

    /**
     * 判断此评估器是否支持该条件的 sourceType
     */
    boolean supports(String sourceType);

    /**
     * 评估条件是否满足
     *
     * @param condition  条件定义
     * @param targetId   被评估目标ID
     * @param targetType 目标类型 (ORG/PLACE/USER)
     * @param cycleStart 评估周期开始日期
     * @param cycleEnd   评估周期结束日期
     * @return 评估结果
     */
    ConditionResult evaluate(EvalCondition condition, Long targetId, String targetType,
                             LocalDate cycleStart, LocalDate cycleEnd);
}
