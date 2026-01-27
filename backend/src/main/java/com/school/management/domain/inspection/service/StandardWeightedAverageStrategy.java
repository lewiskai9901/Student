package com.school.management.domain.inspection.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Standard weighted average strategy.
 * Final score = sum(categoryScore * weight) / totalWeight.
 * Weights are percentages that should add up to 100.
 */
@Component("standardWeightedAverage")
public class StandardWeightedAverageStrategy implements WeightCalculationStrategy {

    private static final int SCALE = 2;

    @Override
    public BigDecimal calculate(BigDecimal baseScore,
                                Map<Long, BigDecimal> categoryScores,
                                Map<Long, BigDecimal> categoryWeights) {
        if (categoryScores.isEmpty()) {
            return baseScore;
        }

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Map.Entry<Long, BigDecimal> entry : categoryScores.entrySet()) {
            BigDecimal weight = categoryWeights.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            weightedSum = weightedSum.add(entry.getValue().multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return baseScore;
        }

        return weightedSum.divide(totalWeight, SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public String getStrategyName() {
        return "STANDARD_WEIGHTED_AVERAGE";
    }
}
