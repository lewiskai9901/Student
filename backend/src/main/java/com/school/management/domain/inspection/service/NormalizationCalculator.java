package com.school.management.domain.inspection.service;

import com.school.management.domain.inspection.model.v7.scoring.NormalizationMode;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 归一化系数计算器
 *
 * 3 种模式:
 * - NONE:           不归一化，系数=1
 * - PER_CAPITA:     人均比例: baseline / population
 * - SQRT_ADJUSTED:  平方根折中: sqrt(baseline / population)
 */
public class NormalizationCalculator {

    private final FormulaEvaluator formulaEvaluator;

    public NormalizationCalculator(FormulaEvaluator formulaEvaluator) {
        this.formulaEvaluator = formulaEvaluator;
    }

    /**
     * 计算归一化系数
     *
     * @param mode               归一化模式
     * @param population         实际人数
     * @param baselinePopulation 基准人数
     * @param floorAt            系数下限 (null则不限)
     * @param cappedAt           系数上限 (null则不限)
     * @param customFormula      CUSTOM 模式的 JS 公式
     * @return 归一化系数，裁剪到 [floorAt, cappedAt]
     */
    public BigDecimal calculate(NormalizationMode mode,
                                int population,
                                int baselinePopulation,
                                BigDecimal floorAt,
                                BigDecimal cappedAt,
                                String customFormula) {

        if (mode == null || mode == NormalizationMode.NONE) {
            return BigDecimal.ONE;
        }
        if (population <= 0) {
            return BigDecimal.ONE;
        }

        BigDecimal pop = BigDecimal.valueOf(population);
        BigDecimal baseline = BigDecimal.valueOf(baselinePopulation);
        BigDecimal factor;

        switch (mode) {
            case PER_CAPITA:
                // baseline / population
                factor = baseline.divide(pop, 10, RoundingMode.HALF_UP);
                break;

            case SQRT_ADJUSTED:
                // sqrt(baseline / population)
                BigDecimal ratio = baseline.divide(pop, 20, RoundingMode.HALF_UP);
                factor = ratio.sqrt(new java.math.MathContext(15));
                break;

            default:
                return BigDecimal.ONE;
        }

        // 裁剪到 [floorAt, cappedAt]
        return clamp(factor, floorAt, cappedAt);
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal floor, BigDecimal cap) {
        if (floor != null && value.compareTo(floor) < 0) {
            value = floor;
        }
        if (cap != null && value.compareTo(cap) > 0) {
            value = cap;
        }
        return value;
    }
}
