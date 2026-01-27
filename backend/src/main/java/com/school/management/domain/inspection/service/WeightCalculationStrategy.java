package com.school.management.domain.inspection.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Strategy interface for calculating weighted inspection scores.
 * Supports two modes:
 * <ul>
 *   <li>Standard weighted average: final = sum(categoryScore * weight) / totalWeight</li>
 *   <li>Category weight: each category contributes independently via score * (weight / 100)</li>
 * </ul>
 */
public interface WeightCalculationStrategy {

    /**
     * Calculates the weighted final score.
     *
     * @param baseScore       the base score before weighting
     * @param categoryScores  key=categoryId, value=score for that category
     * @param categoryWeights key=categoryId, value=weight as a percentage
     * @return the weighted final score
     */
    BigDecimal calculate(BigDecimal baseScore,
                         Map<Long, BigDecimal> categoryScores,
                         Map<Long, BigDecimal> categoryWeights);

    /**
     * Returns the unique name identifying this strategy.
     */
    String getStrategyName();
}
