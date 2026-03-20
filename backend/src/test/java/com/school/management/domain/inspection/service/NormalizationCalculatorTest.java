package com.school.management.domain.inspection.service;

import com.school.management.domain.inspection.model.v7.scoring.NormalizationMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NormalizationCalculator 测试")
class NormalizationCalculatorTest {

    @Mock
    private FormulaEvaluator formulaEvaluator;

    private NormalizationCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new NormalizationCalculator(formulaEvaluator);
    }

    // ==================== NONE 模式 ====================

    @Test
    @DisplayName("NONE模式 — 返回系数1.0，不做归一化")
    void testNone_NoChange() {
        BigDecimal result = calculator.calculate(
                NormalizationMode.NONE,
                50, 40,
                null, null, null);

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("null模式 — 等同NONE，返回系数1.0")
    void testNull_NoChange() {
        BigDecimal result = calculator.calculate(
                null,
                50, 40,
                null, null, null);

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("population为0时 — 返回系数1.0")
    void testZeroPopulation_NoChange() {
        BigDecimal result = calculator.calculate(
                NormalizationMode.PER_CAPITA,
                0, 40,
                null, null, null);

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("population为负数时 — 返回系数1.0")
    void testNegativePopulation_NoChange() {
        BigDecimal result = calculator.calculate(
                NormalizationMode.PER_CAPITA,
                -10, 40,
                null, null, null);

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
    }

    // ==================== PER_CAPITA 模式 ====================

    @Test
    @DisplayName("PER_CAPITA — baseline/population = 40/50 = 0.8")
    void testPerCapita() {
        BigDecimal result = calculator.calculate(
                NormalizationMode.PER_CAPITA,
                50, 40,
                null, null, null);

        // 40 / 50 = 0.8
        assertThat(result).isEqualByComparingTo(new BigDecimal("0.8"));
    }

    @Test
    @DisplayName("PER_CAPITA — 人数等于基准时系数为1")
    void testPerCapita_EqualPopulation() {
        BigDecimal result = calculator.calculate(
                NormalizationMode.PER_CAPITA,
                40, 40,
                null, null, null);

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("PER_CAPITA — 人数少于基准时系数大于1")
    void testPerCapita_SmallerPopulation() {
        BigDecimal result = calculator.calculate(
                NormalizationMode.PER_CAPITA,
                20, 40,
                null, null, null);

        // 40 / 20 = 2.0
        assertThat(result).isEqualByComparingTo(new BigDecimal("2.0"));
    }

    // ==================== SQRT_ADJUSTED 模式 ====================

    @Test
    @DisplayName("SQRT_ADJUSTED — sqrt(baseline/population)")
    void testSqrtAdjusted() {
        BigDecimal result = calculator.calculate(
                NormalizationMode.SQRT_ADJUSTED,
                100, 25,
                null, null, null);

        // sqrt(25/100) = sqrt(0.25) = 0.5
        assertThat(result.setScale(1, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("0.5"));
    }

    @Test
    @DisplayName("SQRT_ADJUSTED — 相同人数时系数为1")
    void testSqrtAdjusted_Equal() {
        BigDecimal result = calculator.calculate(
                NormalizationMode.SQRT_ADJUSTED,
                40, 40,
                null, null, null);

        // sqrt(40/40) = sqrt(1) = 1.0
        assertThat(result.setScale(1, RoundingMode.HALF_UP))
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    // ==================== floor 和 cap 裁剪测试 ====================

    @Test
    @DisplayName("系数裁剪 — 低于floorAt时裁剪到floor")
    void testFloorAndCap_Floor() {
        // PER_CAPITA: 40/200 = 0.2, floorAt = 0.5 => 裁剪到 0.5
        BigDecimal result = calculator.calculate(
                NormalizationMode.PER_CAPITA,
                200, 40,
                new BigDecimal("0.5"), new BigDecimal("2.0"), null);

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.5"));
    }

    @Test
    @DisplayName("系数裁剪 — 超过cappedAt时裁剪到cap")
    void testFloorAndCap_Cap() {
        // PER_CAPITA: 40/10 = 4.0, cappedAt = 2.0 => 裁剪到 2.0
        BigDecimal result = calculator.calculate(
                NormalizationMode.PER_CAPITA,
                10, 40,
                new BigDecimal("0.5"), new BigDecimal("2.0"), null);

        assertThat(result).isEqualByComparingTo(new BigDecimal("2.0"));
    }

    @Test
    @DisplayName("系数裁剪 — 在[floorAt, cappedAt]区间内不裁剪")
    void testFloorAndCap_InRange() {
        // PER_CAPITA: 40/50 = 0.8, floor=0.5, cap=2.0 => 不裁剪
        BigDecimal result = calculator.calculate(
                NormalizationMode.PER_CAPITA,
                50, 40,
                new BigDecimal("0.5"), new BigDecimal("2.0"), null);

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.8"));
    }

    @Test
    @DisplayName("系数裁剪 — floorAt为null时不限下界")
    void testFloorAndCap_NullFloor() {
        // PER_CAPITA: 40/200 = 0.2, floor=null, cap=2.0 => 不裁剪下界
        BigDecimal result = calculator.calculate(
                NormalizationMode.PER_CAPITA,
                200, 40,
                null, new BigDecimal("2.0"), null);

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.2"));
    }

    @Test
    @DisplayName("系数裁剪 — cappedAt为null时不限上界")
    void testFloorAndCap_NullCap() {
        // PER_CAPITA: 40/10 = 4.0, floor=null, cap=null => 不裁剪
        BigDecimal result = calculator.calculate(
                NormalizationMode.PER_CAPITA,
                10, 40,
                null, null, null);

        assertThat(result).isEqualByComparingTo(new BigDecimal("4.0"));
    }
}
