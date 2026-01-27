package com.school.management.domain.inspection.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain service that selects and executes the appropriate weight calculation strategy.
 * Strategies are auto-discovered via Spring dependency injection.
 */
@Slf4j
@Service
public class WeightCalculationService {

    private final Map<String, WeightCalculationStrategy> strategies = new HashMap<>();

    public WeightCalculationService(List<WeightCalculationStrategy> strategyList) {
        for (WeightCalculationStrategy strategy : strategyList) {
            strategies.put(strategy.getStrategyName(), strategy);
        }
        log.info("Registered {} weight calculation strategies: {}", strategies.size(), strategies.keySet());
    }

    /**
     * Calculates weighted score using the specified strategy.
     *
     * @param strategyName    the strategy to use (e.g. "STANDARD_WEIGHTED_AVERAGE" or "CATEGORY_WEIGHT")
     * @param baseScore       the base score before weighting
     * @param categoryScores  key=categoryId, value=score for that category
     * @param categoryWeights key=categoryId, value=weight as a percentage
     * @return the weighted final score
     */
    public BigDecimal calculate(String strategyName, BigDecimal baseScore,
                                Map<Long, BigDecimal> categoryScores,
                                Map<Long, BigDecimal> categoryWeights) {
        WeightCalculationStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            log.warn("Strategy '{}' not found, falling back to STANDARD_WEIGHTED_AVERAGE", strategyName);
            strategy = strategies.get("STANDARD_WEIGHTED_AVERAGE");
        }
        if (strategy == null) {
            log.error("No available calculation strategy, returning base score");
            return baseScore;
        }
        return strategy.calculate(baseScore, categoryScores, categoryWeights);
    }

    /**
     * Returns names of all registered strategies.
     */
    public List<String> getAvailableStrategies() {
        return List.copyOf(strategies.keySet());
    }
}
