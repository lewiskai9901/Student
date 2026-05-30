package com.school.management.domain.inspection.service;

import com.school.management.domain.inspection.model.scoring.*;
import com.school.management.domain.inspection.service.ScoreCalculationDomainService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreCalculationDomainService 测试")
class ScoreCalculationDomainServiceTest {

    @Mock
    private FormulaEvaluator formulaEvaluator;

    private ScoreCalculationDomainService service;

    @BeforeEach
    void setUp() {
        service = new ScoreCalculationDomainService(formulaEvaluator);
    }

    // ==================== 工厂方法 ====================

    private ScoringProfile buildProfile(BigDecimal maxScore, BigDecimal minScore) {
        return ScoringProfile.builder()
                .id(1L)
                .sectionId(100L)
                .maxScore(maxScore)
                .minScore(minScore)
                .precisionDigits(2)
                .createdBy(1L)
                .build();
    }

    private ScoreDimension buildDimension(Long id, String code, String name,
                                           Integer weight, BigDecimal baseScore,
                                           BigDecimal passThreshold) {
        return ScoreDimension.builder()
                .id(id)
                .dimensionCode(code)
                .dimensionName(name)
                .weight(weight)
                .baseScore(baseScore)
                .passThreshold(passThreshold)
                .build();
    }

    // P1.7: 引擎现以 itemScore (= detail.score, 服务端权威) 为项原始分, 不再 switch on mode 重算.
    // 测试 helper 直接给出等价 itemScore: DEDUCTION → -abs(score)*quantity, ADDITION → abs(score)*quantity.
    private ItemScoreInput buildDeductionInput(String itemCode, Long dimensionId,
                                                BigDecimal score, int quantity) {
        BigDecimal itemScore = score.abs().multiply(BigDecimal.valueOf(quantity)).negate();
        return new ItemScoreInput(itemCode, dimensionId, "DEDUCTION",
                score, null, quantity, itemScore, null);
    }

    private ItemScoreInput buildAdditionInput(String itemCode, Long dimensionId,
                                               BigDecimal score, int quantity) {
        BigDecimal itemScore = score.abs().multiply(BigDecimal.valueOf(quantity));
        return new ItemScoreInput(itemCode, dimensionId, "ADDITION",
                score, null, quantity, itemScore, null);
    }

    private GradeBand buildGradeBand(String gradeCode, BigDecimal min, BigDecimal max) {
        return GradeBand.builder()
                .id(1L)
                .gradeCode(gradeCode)
                .gradeName(gradeCode)
                .minScore(min)
                .maxScore(max)
                .dimensionId(null)
                .build();
    }

    // ==================== 基本聚合测试 ====================

    @Nested
    @DisplayName("基本聚合 (baseScore + SUM within dim, WEIGHTED_AVG across dims)")
    class AggregationTests {

        @Test
        @DisplayName("单维度 — 多个扣分项 SUM 聚合")
        void testBasicScoreCalculation_Sum() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), BigDecimal.ZERO);

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("100"), null);

            List<ItemScoreInput> inputs = List.of(
                    buildDeductionInput("ITEM_A", 1L, new BigDecimal("-2"), 1),
                    buildDeductionInput("ITEM_B", 1L, new BigDecimal("-3"), 2)
            );

            ScoreResult result = service.calculate(
                    profile, List.of(dim), Collections.emptyList(),
                    Collections.emptyList(), inputs, 0);

            // dim score = 100 + (-2) + (-6) = 92
            assertThat(result).isNotNull();
            assertThat(result.getFinalScore().compareTo(new BigDecimal("92.00"))).isEqualTo(0);
            assertThat(result.isPassed()).isTrue();
            assertThat(result.getItemOutputs()).hasSize(2);
        }

        @Test
        @DisplayName("单维度 — 加分项 SUM 聚合")
        void testSumAggregation_Addition() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), BigDecimal.ZERO);

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("80"), null);

            List<ItemScoreInput> inputs = List.of(
                    buildAdditionInput("ITEM_A", 1L, new BigDecimal("5"), 1),
                    buildAdditionInput("ITEM_B", 1L, new BigDecimal("3"), 2)
            );

            ScoreResult result = service.calculate(
                    profile, List.of(dim), Collections.emptyList(),
                    Collections.emptyList(), inputs, 0);

            // dim score = 80 + 5 + 6 = 91
            assertThat(result.getFinalScore().compareTo(new BigDecimal("91.00"))).isEqualTo(0);
        }

        @Test
        @DisplayName("多维度加权平均 — dim1(w=60) + dim2(w=40)")
        void testMultiDimension_WeightedAvg() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), BigDecimal.ZERO);

            ScoreDimension dim1 = buildDimension(1L, "DIM1", "卫生", 60,
                    new BigDecimal("100"), null);
            ScoreDimension dim2 = buildDimension(2L, "DIM2", "纪律", 40,
                    new BigDecimal("100"), null);

            List<ItemScoreInput> inputs = List.of(
                    buildDeductionInput("ITEM_A", 1L, new BigDecimal("-10"), 1),
                    buildDeductionInput("ITEM_B", 2L, new BigDecimal("-30"), 1)
            );

            ScoreResult result = service.calculate(
                    profile, List.of(dim1, dim2), Collections.emptyList(),
                    Collections.emptyList(), inputs, 0);

            // total = (90*60 + 70*40) / 100 = 82
            assertThat(result.getDimensionScores()).hasSize(2);
            assertThat(result.getDimensionScores().get(1L).getScore().compareTo(new BigDecimal("90.00"))).isEqualTo(0);
            assertThat(result.getDimensionScores().get(2L).getScore().compareTo(new BigDecimal("70.00"))).isEqualTo(0);
            assertThat(result.getFinalScore().compareTo(new BigDecimal("82.00"))).isEqualTo(0);
        }
    }

    // ==================== P1.7 引擎消费 itemScore ====================

    @Nested
    @DisplayName("P1.7 引擎以 itemScore (=detail.score) 为项原始分")
    class ItemScoreConsumptionTests {

        @Test
        @DisplayName("LEVEL 项 itemScore=8 经引擎 → finalScore 反映 8 (旧 switch 逻辑会算 0)")
        void engineConsumesItemScoreForLevelMode() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), new BigDecimal("-100"));
            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    BigDecimal.ZERO, null);

            // LEVEL 模式 → mapScoringMode 旧实现归 RESPONSE_MAPPED, responseNumericValue=null
            // → 旧 calculateItemScore 会算出 rawScore=0. 现以 itemScore=8 为权威.
            ItemScoreInput input = new ItemScoreInput("LVL_ITEM", 1L, "RESPONSE_MAPPED",
                    BigDecimal.ZERO, null, 1, new BigDecimal("8"), null);

            ScoreResult result = service.calculate(
                    profile, List.of(dim), Collections.emptyList(),
                    Collections.emptyList(), List.of(input), 0);

            // dimScore = base 0 + itemScore 8 = 8
            assertThat(result.getFinalScore()).isEqualByComparingTo("8.00");
            assertThat(result.getItemOutputs().get(0).getRawScore()).isEqualByComparingTo("8");
        }

        @Test
        @DisplayName("itemScore=null → 当 0, 不抛 NPE")
        void nullItemScoreTreatedAsZero() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), new BigDecimal("-100"));
            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("50"), null);

            ItemScoreInput input = new ItemScoreInput("X", 1L, "DEDUCTION",
                    BigDecimal.ZERO, null, 1, null, null);

            ScoreResult result = service.calculate(
                    profile, List.of(dim), Collections.emptyList(),
                    Collections.emptyList(), List.of(input), 0);

            // dimScore = base 50 + 0 = 50
            assertThat(result.getFinalScore()).isEqualByComparingTo("50.00");
        }
    }

    // ==================== 归一化测试 ====================

    @Nested
    @DisplayName("归一化计算")
    class NormalizationTests {

        @Test
        @DisplayName("PER_CAPITA 归一化 — 扣分按人均比例调整")
        void testScoreCalculation_WithNormalization_PerCapita() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), BigDecimal.ZERO);

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("100"), null);

            NormalizationConfig normConfig = new NormalizationConfig(
                    true, NormalizationMode.PER_CAPITA, 40,
                    new BigDecimal("2.0"), new BigDecimal("0.5"), null);

            // itemScore = -6 (= -abs(2)*3 服务端权威算出)
            ItemScoreInput input = new ItemScoreInput("ITEM_A", 1L, "DEDUCTION",
                    new BigDecimal("-2"), null, 3, new BigDecimal("-6"), normConfig);

            ScoreResult result = service.calculate(
                    profile, List.of(dim), Collections.emptyList(),
                    Collections.emptyList(), List.of(input), 50);

            // rawScore = -6, normFactor = 0.8, finalItemScore = -4.8
            // dimScore = 100 + (-4.8) = 95.2
            assertThat(result.getFinalScore().compareTo(new BigDecimal("95.20"))).isEqualTo(0);
            assertThat(result.getItemOutputs().get(0).getRawScore()
                    .compareTo(new BigDecimal("-6"))).isEqualTo(0);
        }

        @Test
        @DisplayName("PER_CAPITA 归一化 — 不同 baseline/population 比例")
        void testScoreCalculation_WithNormalization_PerCapita_HalfRatio() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), BigDecimal.ZERO);

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("100"), null);

            NormalizationConfig normConfig = new NormalizationConfig(
                    true, NormalizationMode.PER_CAPITA, 40,
                    new BigDecimal("2.0"), new BigDecimal("0.1"), null);

            // itemScore = -10 (= -abs(10)*1)
            ItemScoreInput input = new ItemScoreInput("ITEM_A", 1L, "DEDUCTION",
                    new BigDecimal("-10"), null, 1, new BigDecimal("-10"), normConfig);

            ScoreResult result = service.calculate(
                    profile, List.of(dim), Collections.emptyList(),
                    Collections.emptyList(), List.of(input), 80);

            // rawScore = -10, normFactor = 0.5, finalItemScore = -5
            // dimScore = 100 + (-5) = 95
            assertThat(result.getFinalScore().compareTo(new BigDecimal("95.00"))).isEqualTo(0);
        }
    }

    // ==================== 等级映射测试 ====================

    @Nested
    @DisplayName("等级映射")
    class GradeBandTests {

        @Test
        @DisplayName("分数映射到正确等级")
        void testGradeBandMapping() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), BigDecimal.ZERO);

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("100"), null);

            List<GradeBand> gradeBands = List.of(
                    buildGradeBand("A", new BigDecimal("90"), new BigDecimal("100")),
                    buildGradeBand("B", new BigDecimal("80"), new BigDecimal("89.99")),
                    buildGradeBand("C", new BigDecimal("60"), new BigDecimal("79.99")),
                    buildGradeBand("D", new BigDecimal("0"), new BigDecimal("59.99"))
            );

            List<ItemScoreInput> inputs = List.of(
                    buildDeductionInput("ITEM_A", 1L, new BigDecimal("-15"), 1)
            );

            ScoreResult result = service.calculate(
                    profile, List.of(dim), Collections.emptyList(),
                    gradeBands, inputs, 0);

            // 100 + (-15) = 85 => B
            assertThat(result.getFinalScore().compareTo(new BigDecimal("85.00"))).isEqualTo(0);
            assertThat(result.getGrade()).isEqualTo("B");
        }

        @Test
        @DisplayName("满分映射到A等级")
        void testGradeBandMapping_FullScore() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), BigDecimal.ZERO);

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("100"), null);

            List<GradeBand> gradeBands = List.of(
                    buildGradeBand("A", new BigDecimal("90"), new BigDecimal("100")),
                    buildGradeBand("B", new BigDecimal("80"), new BigDecimal("89.99"))
            );

            ScoreResult result = service.calculate(
                    profile, List.of(dim), Collections.emptyList(),
                    gradeBands, Collections.emptyList(), 0);

            assertThat(result.getFinalScore().compareTo(new BigDecimal("100.00"))).isEqualTo(0);
            assertThat(result.getGrade()).isEqualTo("A");
        }
    }

    // ==================== 规则链测试 ====================

    @Nested
    @DisplayName("规则链执行")
    class RuleChainTests {

        @Test
        @DisplayName("VETO 一票否决规则 — 触发时分数归零")
        void testVetoRule() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), BigDecimal.ZERO);

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("100"), null);

            List<ItemScoreInput> inputs = List.of(
                    buildDeductionInput("VETO_ITEM", 1L, new BigDecimal("-5"), 1)
            );

            CalculationRule vetoRule = CalculationRule.builder()
                    .id(1L)
                    .ruleCode("VETO_SAFETY")
                    .ruleType(RuleType.VETO)
                    .priority(1)
                    .isEnabled(true)
                    .config("{\"vetoItems\": [\"VETO_ITEM\"], \"vetoScore\": \"0\"}")
                    .build();

            ScoreResult result = service.calculate(
                    profile, List.of(dim), List.of(vetoRule),
                    Collections.emptyList(), inputs, 0);

            assertThat(result.getFinalScore().compareTo(BigDecimal.ZERO)).isEqualTo(0);
            assertThat(result.getRuleApplications().get(0).isApplied()).isTrue();
        }

        @Test
        @DisplayName("BONUS 奖励加分规则")
        void testBonusRule() {
            ScoringProfile profile = buildProfile(new BigDecimal("120"), BigDecimal.ZERO);

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("100"), null);

            List<ItemScoreInput> inputs = List.of(
                    buildAdditionInput("BONUS_A", 1L, new BigDecimal("3"), 1),
                    buildAdditionInput("BONUS_B", 1L, new BigDecimal("2"), 1)
            );

            CalculationRule bonusRule = CalculationRule.builder()
                    .id(1L)
                    .ruleCode("BONUS_EXTRA")
                    .ruleType(RuleType.BONUS)
                    .priority(1)
                    .isEnabled(true)
                    .config("{\"bonusItems\": [\"BONUS_A\", \"BONUS_B\"], \"bonusScore\": \"1\"}")
                    .build();

            ScoreResult result = service.calculate(
                    profile, List.of(dim), List.of(bonusRule),
                    Collections.emptyList(), inputs, 0);

            assertThat(result.getRuleApplications().get(0).isApplied()).isTrue();
            assertThat(result.getBonusTotal().compareTo(new BigDecimal("2.00"))).isEqualTo(0);
        }

        @Test
        @DisplayName("PENALTY 扣分规则 — 命中每个不合格项各扣 penaltyScore")
        void applyRule_penalty_deductsPerMatchedItem() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), new BigDecimal("-100"));

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("100"), null);

            // I1, I2 不合格 (DEDUCTION → finalScore != 0); I3 合格 (finalScore == 0)
            List<ItemScoreInput> inputs = List.of(
                    buildDeductionInput("I1", 1L, new BigDecimal("-1"), 1),
                    buildDeductionInput("I2", 1L, new BigDecimal("-1"), 1),
                    buildDeductionInput("I3", 1L, new BigDecimal("0"), 1)
            );

            CalculationRule penaltyRule = CalculationRule.builder()
                    .id(1L)
                    .ruleCode("PENALTY_RULE")
                    .ruleType(RuleType.PENALTY)
                    .priority(1)
                    .isEnabled(true)
                    .config("{\"penaltyItems\": [\"I1\", \"I2\"], \"penaltyScore\": \"5\"}")
                    .build();

            ScoreResult result = service.calculate(
                    profile, List.of(dim), List.of(penaltyRule),
                    Collections.emptyList(), inputs, 0);

            RuleApplication app = result.getRuleApplications().stream()
                    .filter(a -> a.getRuleType() == RuleType.PENALTY)
                    .findFirst()
                    .orElseThrow();
            assertThat(app.isApplied()).isTrue();
            // 命中 2 项 (I1, I2)，每项扣 5 → -10
            assertThat(app.getAdjustment()).isEqualByComparingTo("-10");
            // 端到端: 扣分流入最终分
            // deductionTotal 累计所有负向 adjustment = -10
            assertThat(result.getDeductionTotal()).isEqualByComparingTo("-10");
            // 维度分 = base 100 + I1(-1) + I2(-1) + I3(0) = 98; 再被 PENALTY 扣 10 = 88
            assertThat(result.getFinalScore()).isEqualByComparingTo("88");
        }

        @Test
        @DisplayName("PROGRESSIVE_BONUS 累进加分规则 — 达标项数命中最大阈值档")
        void applyRule_progressiveBonus_addsByThreshold() {
            // maxScore 足够大避免 clamp 截掉加分: 维度分 112 + bonus 6 = 118
            ScoringProfile profile = buildProfile(new BigDecimal("130"), new BigDecimal("-100"));

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("100"), null);

            // 4 个合格(加分)项, finalScore = 3 > 0 each
            List<ItemScoreInput> inputs = List.of(
                    buildAdditionInput("Q1", 1L, new BigDecimal("3"), 1),
                    buildAdditionInput("Q2", 1L, new BigDecimal("3"), 1),
                    buildAdditionInput("Q3", 1L, new BigDecimal("3"), 1),
                    buildAdditionInput("Q4", 1L, new BigDecimal("3"), 1)
            );

            CalculationRule progressiveBonusRule = CalculationRule.builder()
                    .id(1L)
                    .ruleCode("PROGRESSIVE_BONUS_RULE")
                    .ruleType(RuleType.PROGRESSIVE_BONUS)
                    .priority(1)
                    .isEnabled(true)
                    .config("{\"thresholds\": [{\"count\": 2, \"bonus\": 3}, {\"count\": 4, \"bonus\": 6}]}")
                    .build();

            ScoreResult result = service.calculate(
                    profile, List.of(dim), List.of(progressiveBonusRule),
                    Collections.emptyList(), inputs, 0);

            RuleApplication app = result.getRuleApplications().stream()
                    .filter(a -> a.getRuleType() == RuleType.PROGRESSIVE_BONUS)
                    .findFirst()
                    .orElseThrow();
            assertThat(app.isApplied()).isTrue();
            // 4 项达标 → 命中 count>=4 档 → +6 (加分, 不 negate)
            assertThat(app.getAdjustment()).isEqualByComparingTo("6");
            // 端到端: 加分流入 bonusTotal
            assertThat(result.getBonusTotal()).isEqualByComparingTo("6");
        }

        @Test
        @DisplayName("PROGRESSIVE_BONUS 累进加分规则 — 0 达标项不应用 (钉死 >0 谓词)")
        void applyRule_progressiveBonus_noQualifiedItems_notApplied() {
            ScoringProfile profile = buildProfile(new BigDecimal("130"), new BigDecimal("-100"));

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("100"), null);

            // 没有任何合格项: 全是扣分项/0 分项 → finalScore > 0 的项数为 0
            List<ItemScoreInput> inputs = List.of(
                    buildDeductionInput("D1", 1L, new BigDecimal("-1"), 1),
                    buildDeductionInput("D2", 1L, new BigDecimal("-1"), 1),
                    buildDeductionInput("Z1", 1L, new BigDecimal("0"), 1)
            );

            CalculationRule progressiveBonusRule = CalculationRule.builder()
                    .id(1L)
                    .ruleCode("PROGRESSIVE_BONUS_RULE")
                    .ruleType(RuleType.PROGRESSIVE_BONUS)
                    .priority(1)
                    .isEnabled(true)
                    .config("{\"thresholds\": [{\"count\": 2, \"bonus\": 3}, {\"count\": 4, \"bonus\": 6}]}")
                    .build();

            ScoreResult result = service.calculate(
                    profile, List.of(dim), List.of(progressiveBonusRule),
                    Collections.emptyList(), inputs, 0);

            RuleApplication app = result.getRuleApplications().stream()
                    .filter(a -> a.getRuleType() == RuleType.PROGRESSIVE_BONUS)
                    .findFirst()
                    .orElseThrow();
            // 0 项达标 → 不命中任何阈值档 → 规则不应用
            assertThat(app.isApplied()).isFalse();
            // 无加分流入 bonusTotal
            assertThat(result.getBonusTotal()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("禁用的规则不执行")
        void testDisabledRuleSkipped() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), BigDecimal.ZERO);

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("100"), null);

            List<ItemScoreInput> inputs = List.of(
                    buildAdditionInput("ITEM_A", 1L, new BigDecimal("15"), 1)
            );

            CalculationRule disabledRule = CalculationRule.builder()
                    .id(1L)
                    .ruleCode("VETO_TEST")
                    .ruleType(RuleType.VETO)
                    .priority(1)
                    .isEnabled(false)
                    .config("{\"vetoItems\": [\"ITEM_A\"], \"vetoScore\": \"0\"}")
                    .build();

            ScoreResult result = service.calculate(
                    profile, List.of(dim), List.of(disabledRule),
                    Collections.emptyList(), inputs, 0);

            assertThat(result.getRuleApplications()).isEmpty();
        }
    }

    // ==================== 分数范围裁剪测试 ====================

    @Nested
    @DisplayName("分数范围裁剪")
    class ScoreClampTests {

        @Test
        @DisplayName("分数不低于 minScore")
        void testClampToMinScore() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), new BigDecimal("20"));

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("100"), null);

            List<ItemScoreInput> inputs = List.of(
                    buildDeductionInput("ITEM_A", 1L, new BigDecimal("-90"), 1)
            );

            ScoreResult result = service.calculate(
                    profile, List.of(dim), Collections.emptyList(),
                    Collections.emptyList(), inputs, 0);

            assertThat(result.getFinalScore().compareTo(new BigDecimal("20.00"))).isEqualTo(0);
        }

        @Test
        @DisplayName("分数不超过 maxScore")
        void testClampToMaxScore() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), BigDecimal.ZERO);

            ScoreDimension dim = buildDimension(1L, "DIM1", "维度1", 100,
                    new BigDecimal("100"), null);

            List<ItemScoreInput> inputs = List.of(
                    buildAdditionInput("ITEM_A", 1L, new BigDecimal("20"), 1)
            );

            ScoreResult result = service.calculate(
                    profile, List.of(dim), Collections.emptyList(),
                    Collections.emptyList(), inputs, 0);

            assertThat(result.getFinalScore().compareTo(new BigDecimal("100.00"))).isEqualTo(0);
        }
    }

    // ==================== 维度通过阈值测试 ====================

    @Nested
    @DisplayName("维度通过判定")
    class DimensionPassTests {

        @Test
        @DisplayName("维度分数低于阈值时 passed=false")
        void testDimensionFailsThreshold() {
            ScoringProfile profile = buildProfile(new BigDecimal("100"), BigDecimal.ZERO);

            ScoreDimension dim = buildDimension(1L, "DIM1", "安全", 100,
                    new BigDecimal("100"), new BigDecimal("80"));

            List<ItemScoreInput> inputs = List.of(
                    buildDeductionInput("ITEM_A", 1L, new BigDecimal("-25"), 1)
            );

            ScoreResult result = service.calculate(
                    profile, List.of(dim), Collections.emptyList(),
                    Collections.emptyList(), inputs, 0);

            assertThat(result.getDimensionScores().get(1L).isPassed()).isFalse();
            assertThat(result.isPassed()).isFalse();
        }
    }
}
