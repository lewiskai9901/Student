package com.school.management.domain.inspection.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the weight calculation domain service.
 *
 * Covers:
 * - StandardWeightedAverageStrategy: equal weights, unequal weights, zero weights, empty scores
 * - CategoryWeightStrategy: standard calculation, weight-as-percentage, empty scores
 * - WeightCalculationService: strategy dispatch, fallback behavior, empty category scores
 */
@DisplayName("Weight Calculation Domain Service")
class WeightCalculationServiceTest {

    private StandardWeightedAverageStrategy standardStrategy;
    private CategoryWeightStrategy categoryStrategy;
    private WeightCalculationService service;

    @BeforeEach
    void setUp() {
        standardStrategy = new StandardWeightedAverageStrategy();
        categoryStrategy = new CategoryWeightStrategy();
        service = new WeightCalculationService(List.of(standardStrategy, categoryStrategy));
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Map<Long, BigDecimal> scoresOf(Object... keyValues) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            Long key = ((Number) keyValues[i]).longValue();
            BigDecimal value = new BigDecimal(keyValues[i + 1].toString());
            map.put(key, value);
        }
        return map;
    }

    private Map<Long, BigDecimal> weightsOf(Object... keyValues) {
        return scoresOf(keyValues); // same structure
    }

    // =========================================================================
    // StandardWeightedAverageStrategy Tests
    // =========================================================================

    @Nested
    @DisplayName("StandardWeightedAverageStrategy")
    class StandardWeightedAverageStrategyTest {

        @Test
        @DisplayName("should return correct strategy name")
        void shouldReturnCorrectStrategyName() {
            assertEquals("STANDARD_WEIGHTED_AVERAGE", standardStrategy.getStrategyName());
        }

        @Test
        @DisplayName("should calculate weighted average with equal weights")
        void shouldCalculateWithEqualWeights() {
            // given: three categories, each weight=50
            // scores: cat1=90, cat2=80, cat3=70
            // weights: cat1=50, cat2=50, cat3=50
            // expected: (90*50 + 80*50 + 70*50) / (50+50+50) = 12000 / 150 = 80.00
            Map<Long, BigDecimal> scores = scoresOf(1L, "90", 2L, "80", 3L, "70");
            Map<Long, BigDecimal> weights = weightsOf(1L, "50", 2L, "50", 3L, "50");

            // when
            BigDecimal result = standardStrategy.calculate(new BigDecimal("100"), scores, weights);

            // then
            assertEquals(new BigDecimal("80.00"), result);
        }

        @Test
        @DisplayName("should calculate weighted average with unequal weights")
        void shouldCalculateWithUnequalWeights() {
            // given: two categories
            // scores: cat1=90, cat2=60
            // weights: cat1=70, cat2=30
            // expected: (90*70 + 60*30) / (70+30) = (6300 + 1800) / 100 = 81.00
            Map<Long, BigDecimal> scores = scoresOf(1L, "90", 2L, "60");
            Map<Long, BigDecimal> weights = weightsOf(1L, "70", 2L, "30");

            // when
            BigDecimal result = standardStrategy.calculate(new BigDecimal("100"), scores, weights);

            // then
            assertEquals(new BigDecimal("81.00"), result);
        }

        @Test
        @DisplayName("should handle single category")
        void shouldHandleSingleCategory() {
            // given: one category with score=85, weight=100
            // expected: 85*100 / 100 = 85.00
            Map<Long, BigDecimal> scores = scoresOf(1L, "85");
            Map<Long, BigDecimal> weights = weightsOf(1L, "100");

            // when
            BigDecimal result = standardStrategy.calculate(new BigDecimal("100"), scores, weights);

            // then
            assertEquals(new BigDecimal("85.00"), result);
        }

        @Test
        @DisplayName("should return base score when category scores are empty")
        void shouldReturnBaseScoreWhenEmpty() {
            // given
            Map<Long, BigDecimal> scores = Collections.emptyMap();
            Map<Long, BigDecimal> weights = Collections.emptyMap();
            BigDecimal baseScore = new BigDecimal("100");

            // when
            BigDecimal result = standardStrategy.calculate(baseScore, scores, weights);

            // then
            assertEquals(baseScore, result);
        }

        @Test
        @DisplayName("should return base score when all weights are zero")
        void shouldReturnBaseScoreWhenAllWeightsZero() {
            // given: weights are all zero => totalWeight=0 => fallback to baseScore
            Map<Long, BigDecimal> scores = scoresOf(1L, "90", 2L, "80");
            Map<Long, BigDecimal> weights = weightsOf(1L, "0", 2L, "0");
            BigDecimal baseScore = new BigDecimal("100");

            // when
            BigDecimal result = standardStrategy.calculate(baseScore, scores, weights);

            // then
            assertEquals(baseScore, result);
        }

        @Test
        @DisplayName("should use zero weight when category has no matching weight entry")
        void shouldUseZeroForMissingWeight() {
            // given: score for cat1 and cat2, but weight only for cat1
            // cat2 gets weight=0 by default
            // expected: (90*50 + 80*0) / (50+0) = 4500 / 50 = 90.00
            Map<Long, BigDecimal> scores = scoresOf(1L, "90", 2L, "80");
            Map<Long, BigDecimal> weights = weightsOf(1L, "50");

            // when
            BigDecimal result = standardStrategy.calculate(new BigDecimal("100"), scores, weights);

            // then
            assertEquals(new BigDecimal("90.00"), result);
        }

        @Test
        @DisplayName("should round to 2 decimal places using HALF_UP")
        void shouldRoundToTwoDecimalPlaces() {
            // given: calculation that produces non-terminating decimal
            // scores: cat1=91, cat2=82, cat3=73
            // weights: cat1=33, cat2=33, cat3=34
            // numerator: 91*33 + 82*33 + 73*34 = 3003 + 2706 + 2482 = 8191
            // denominator: 33+33+34 = 100
            // expected: 8191/100 = 81.91
            Map<Long, BigDecimal> scores = scoresOf(1L, "91", 2L, "82", 3L, "73");
            Map<Long, BigDecimal> weights = weightsOf(1L, "33", 2L, "33", 3L, "34");

            // when
            BigDecimal result = standardStrategy.calculate(new BigDecimal("100"), scores, weights);

            // then
            assertEquals(new BigDecimal("81.91"), result);
        }

        @Test
        @DisplayName("should handle decimal scores and weights")
        void shouldHandleDecimalValues() {
            // given: cat1 score=87.5, weight=60.5; cat2 score=92.3, weight=39.5
            // numerator: 87.5*60.5 + 92.3*39.5 = 5293.75 + 3645.85 = 8939.60
            // denominator: 60.5 + 39.5 = 100
            // expected: 8939.60 / 100 = 89.40
            Map<Long, BigDecimal> scores = scoresOf(1L, "87.5", 2L, "92.3");
            Map<Long, BigDecimal> weights = weightsOf(1L, "60.5", 2L, "39.5");

            // when
            BigDecimal result = standardStrategy.calculate(new BigDecimal("100"), scores, weights);

            // then
            assertEquals(new BigDecimal("89.40"), result);
        }
    }

    // =========================================================================
    // CategoryWeightStrategy Tests
    // =========================================================================

    @Nested
    @DisplayName("CategoryWeightStrategy")
    class CategoryWeightStrategyTest {

        @Test
        @DisplayName("should return correct strategy name")
        void shouldReturnCorrectStrategyName() {
            assertEquals("CATEGORY_WEIGHT", categoryStrategy.getStrategyName());
        }

        @Test
        @DisplayName("should calculate category-weighted score with percentage weights")
        void shouldCalculateWithPercentageWeights() {
            // given: score * (weight / 100) for each category
            // cat1: 90 * (40/100) = 36.00
            // cat2: 80 * (60/100) = 48.00
            // total: 84.00
            Map<Long, BigDecimal> scores = scoresOf(1L, "90", 2L, "80");
            Map<Long, BigDecimal> weights = weightsOf(1L, "40", 2L, "60");

            // when
            BigDecimal result = categoryStrategy.calculate(new BigDecimal("100"), scores, weights);

            // then
            assertEquals(new BigDecimal("84.00"), result);
        }

        @Test
        @DisplayName("should sum to original scores when all weights are 100%")
        void shouldEqualSumWhenAllWeightsAre100Percent() {
            // given: each category weight is 100 => score * (100/100) = score
            // cat1: 90, cat2: 80 => total: 170
            Map<Long, BigDecimal> scores = scoresOf(1L, "90", 2L, "80");
            Map<Long, BigDecimal> weights = weightsOf(1L, "100", 2L, "100");

            // when
            BigDecimal result = categoryStrategy.calculate(new BigDecimal("100"), scores, weights);

            // then
            assertEquals(new BigDecimal("170.00"), result);
        }

        @Test
        @DisplayName("should handle single category")
        void shouldHandleSingleCategory() {
            // given: cat1 score=95, weight=50 => 95 * (50/100) = 47.50
            Map<Long, BigDecimal> scores = scoresOf(1L, "95");
            Map<Long, BigDecimal> weights = weightsOf(1L, "50");

            // when
            BigDecimal result = categoryStrategy.calculate(new BigDecimal("100"), scores, weights);

            // then
            assertEquals(new BigDecimal("47.50"), result);
        }

        @Test
        @DisplayName("should return base score when category scores are empty")
        void shouldReturnBaseScoreWhenEmpty() {
            // given
            BigDecimal baseScore = new BigDecimal("100");

            // when
            BigDecimal result = categoryStrategy.calculate(
                baseScore, Collections.emptyMap(), Collections.emptyMap()
            );

            // then
            assertEquals(baseScore, result);
        }

        @Test
        @DisplayName("should default to 100% weight when category has no matching weight entry")
        void shouldDefaultTo100PercentForMissingWeight() {
            // given: cat1 has no weight entry => defaults to 100
            // cat1: 85 * (100/100) = 85.00
            Map<Long, BigDecimal> scores = scoresOf(1L, "85");
            Map<Long, BigDecimal> weights = Collections.emptyMap();

            // when
            BigDecimal result = categoryStrategy.calculate(new BigDecimal("100"), scores, weights);

            // then
            assertEquals(new BigDecimal("85.00"), result);
        }

        @Test
        @DisplayName("should round intermediate results to 2 decimal places")
        void shouldRoundToTwoDecimalPlaces() {
            // given: cat1 score=91, weight=33 => 91 * 33 / 100 = 3003 / 100 = 30.03
            // cat2: score=82, weight=33 => 82 * 33 / 100 = 2706 / 100 = 27.06
            // cat3: score=73, weight=34 => 73 * 34 / 100 = 2482 / 100 = 24.82
            // total: 30.03 + 27.06 + 24.82 = 81.91
            Map<Long, BigDecimal> scores = scoresOf(1L, "91", 2L, "82", 3L, "73");
            Map<Long, BigDecimal> weights = weightsOf(1L, "33", 2L, "33", 3L, "34");

            // when
            BigDecimal result = categoryStrategy.calculate(new BigDecimal("100"), scores, weights);

            // then
            assertEquals(new BigDecimal("81.91"), result);
        }

        @Test
        @DisplayName("should return zero when all weights are zero")
        void shouldReturnZeroWhenAllWeightsZero() {
            // given: every category gets weight 0 => score * (0/100) = 0 for each
            Map<Long, BigDecimal> scores = scoresOf(1L, "90", 2L, "80");
            Map<Long, BigDecimal> weights = weightsOf(1L, "0", 2L, "0");

            // when
            BigDecimal result = categoryStrategy.calculate(new BigDecimal("100"), scores, weights);

            // then
            assertEquals(0, result.compareTo(BigDecimal.ZERO));
        }
    }

    // =========================================================================
    // WeightCalculationService Tests
    // =========================================================================

    @Nested
    @DisplayName("WeightCalculationService (Strategy Dispatch)")
    class ServiceDispatchTest {

        @Test
        @DisplayName("should dispatch to STANDARD_WEIGHTED_AVERAGE strategy")
        void shouldDispatchToStandardStrategy() {
            // given
            Map<Long, BigDecimal> scores = scoresOf(1L, "90", 2L, "80");
            Map<Long, BigDecimal> weights = weightsOf(1L, "50", 2L, "50");

            // when
            BigDecimal result = service.calculate(
                "STANDARD_WEIGHTED_AVERAGE",
                new BigDecimal("100"), scores, weights
            );

            // then - (90*50+80*50)/(50+50) = 85.00
            assertEquals(new BigDecimal("85.00"), result);
        }

        @Test
        @DisplayName("should dispatch to CATEGORY_WEIGHT strategy")
        void shouldDispatchToCategoryStrategy() {
            // given
            Map<Long, BigDecimal> scores = scoresOf(1L, "90", 2L, "80");
            Map<Long, BigDecimal> weights = weightsOf(1L, "40", 2L, "60");

            // when
            BigDecimal result = service.calculate(
                "CATEGORY_WEIGHT",
                new BigDecimal("100"), scores, weights
            );

            // then - 90*(40/100)+80*(60/100) = 36+48 = 84.00
            assertEquals(new BigDecimal("84.00"), result);
        }

        @Test
        @DisplayName("should fallback to STANDARD_WEIGHTED_AVERAGE when strategy not found")
        void shouldFallbackWhenStrategyNotFound() {
            // given
            Map<Long, BigDecimal> scores = scoresOf(1L, "90", 2L, "80");
            Map<Long, BigDecimal> weights = weightsOf(1L, "50", 2L, "50");

            // when - use a non-existent strategy name
            BigDecimal result = service.calculate(
                "NON_EXISTENT_STRATEGY",
                new BigDecimal("100"), scores, weights
            );

            // then - falls back to STANDARD_WEIGHTED_AVERAGE
            assertEquals(new BigDecimal("85.00"), result);
        }

        @Test
        @DisplayName("should return base score when no strategies are registered and strategy not found")
        void shouldReturnBaseScoreWhenNoStrategiesAvailable() {
            // given - service with no strategies
            WeightCalculationService emptyService = new WeightCalculationService(Collections.emptyList());
            BigDecimal baseScore = new BigDecimal("100");

            // when
            BigDecimal result = emptyService.calculate(
                "ANY_STRATEGY", baseScore, scoresOf(1L, "90"), weightsOf(1L, "50")
            );

            // then
            assertEquals(baseScore, result);
        }

        @Test
        @DisplayName("should handle empty category scores")
        void shouldHandleEmptyCategoryScores() {
            // given
            BigDecimal baseScore = new BigDecimal("100");

            // when
            BigDecimal result = service.calculate(
                "STANDARD_WEIGHTED_AVERAGE",
                baseScore,
                Collections.emptyMap(),
                Collections.emptyMap()
            );

            // then - empty scores => returns baseScore
            assertEquals(baseScore, result);
        }

        @Test
        @DisplayName("should list available strategy names")
        void shouldListAvailableStrategies() {
            // when
            List<String> strategies = service.getAvailableStrategies();

            // then
            assertEquals(2, strategies.size());
            assertTrue(strategies.contains("STANDARD_WEIGHTED_AVERAGE"));
            assertTrue(strategies.contains("CATEGORY_WEIGHT"));
        }

        @Test
        @DisplayName("should report empty strategies for service with no strategies")
        void shouldReportEmptyStrategies() {
            // given
            WeightCalculationService emptyService = new WeightCalculationService(Collections.emptyList());

            // when
            List<String> strategies = emptyService.getAvailableStrategies();

            // then
            assertTrue(strategies.isEmpty());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTest {

        @Test
        @DisplayName("should produce consistent results between strategies when weights total 100")
        void shouldProduceConsistentResultsWhenWeightsTotal100() {
            // When weights exactly total 100 and are equal, standard weighted average
            // result = sum(score * weight) / totalWeight
            // category weight result = sum(score * weight / 100)
            // These should be equal in this case.
            Map<Long, BigDecimal> scores = scoresOf(1L, "90", 2L, "80");
            Map<Long, BigDecimal> weights = weightsOf(1L, "50", 2L, "50");

            BigDecimal standardResult = standardStrategy.calculate(new BigDecimal("100"), scores, weights);
            BigDecimal categoryResult = categoryStrategy.calculate(new BigDecimal("100"), scores, weights);

            // standard: (90*50+80*50)/100 = 8500/100 = 85.00
            // category: 90*50/100 + 80*50/100 = 45.00 + 40.00 = 85.00
            assertEquals(standardResult, categoryResult);
        }

        @Test
        @DisplayName("should handle very large scores")
        void shouldHandleLargeScores() {
            Map<Long, BigDecimal> scores = scoresOf(1L, "999999", 2L, "888888");
            Map<Long, BigDecimal> weights = weightsOf(1L, "50", 2L, "50");

            BigDecimal result = standardStrategy.calculate(new BigDecimal("1000000"), scores, weights);

            // (999999*50+888888*50)/100 = 94444350/100 = 944443.50
            assertEquals(new BigDecimal("944443.50"), result);
        }

        @Test
        @DisplayName("should handle zero scores")
        void shouldHandleZeroScores() {
            Map<Long, BigDecimal> scores = scoresOf(1L, "0", 2L, "0");
            Map<Long, BigDecimal> weights = weightsOf(1L, "50", 2L, "50");

            BigDecimal result = standardStrategy.calculate(new BigDecimal("100"), scores, weights);

            assertEquals(new BigDecimal("0.00"), result);
        }
    }
}
