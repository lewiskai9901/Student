package com.school.management.domain.inspection.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Category weight strategy.
 * Each category contributes independently: score * (weight / 100).
 * Allows independent scaling of categories where each category's
 * base score ratio determines its proportional contribution.
 */
@Component("categoryWeight")
public class CategoryWeightStrategy implements WeightCalculationStrategy {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 2;

    @Override
    public BigDecimal calculate(BigDecimal baseScore,
                                Map<Long, BigDecimal> categoryScores,
                                Map<Long, BigDecimal> categoryWeights) {
        if (categoryScores.isEmpty()) {
            return baseScore;
        }

        BigDecimal totalWeightedScore = BigDecimal.ZERO;

        for (Map.Entry<Long, BigDecimal> entry : categoryScores.entrySet()) {
            Long categoryId = entry.getKey();
            BigDecimal score = entry.getValue();
            BigDecimal weight = categoryWeights.getOrDefault(categoryId, HUNDRED);

            // Apply category weight: score * (weight / 100)
            BigDecimal weightedScore = score.multiply(weight)
                    .divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
            totalWeightedScore = totalWeightedScore.add(weightedScore);
        }

        return totalWeightedScore;
    }

    @Override
    public String getStrategyName() {
        return "CATEGORY_WEIGHT";
    }
}
