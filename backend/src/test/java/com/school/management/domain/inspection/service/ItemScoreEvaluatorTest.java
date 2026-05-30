package com.school.management.domain.inspection.service;

import com.school.management.domain.inspection.model.execution.ScoringMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ItemScoreEvaluator 单测 — 8 种确定性评分模式 (P1.1-1.3).
 * 契约依据: TaskExecutionView.vue handler + ItemEditor.vue serializeScoringConfig
 * + 后端 ScoreAggregationService.parseConfigScore.
 */
class ItemScoreEvaluatorTest {

    private final ItemScoreEvaluator ev = new ItemScoreEvaluator();

    /** 注入了真 GraalVM 公式求值器的实例, 用于 FORMULA 模式断言. */
    private final ItemScoreEvaluator evWithFormula = new ItemScoreEvaluator(
            new com.school.management.infrastructure.scoring.GraalVmFormulaEvaluator());

    // ==================== DEDUCTION (responseValue=检查员直接拨的扣分值, 结果取负) ====================
    // 前端 TaskExecutionView.handleDeductionSelect: getDeductionRange.min = -max, step 钳到 [min,0],
    // responseValue=String(val) 存的是负数 (如 "-3"). 语义=直接取值, 不再乘数量. -abs() 保证恒为负.

    @Test
    void deduction_takesResponseValueDirectlyAsNegative() {
        // 前端存负数 "-3" → -3
        assertThat(ev.scoreItem(ScoringMode.DEDUCTION, "-3", "{}"))
                .isEqualByComparingTo("-3");
    }

    @Test
    void deduction_positiveResponseStillNegated() {
        // 即便存成正数 "3" 也取 -abs → -3 (兜底)
        assertThat(ev.scoreItem(ScoringMode.DEDUCTION, "3", "{}"))
                .isEqualByComparingTo("-3");
    }

    @Test
    void deduction_emptyResponseIsZero() {
        assertThat(ev.scoreItem(ScoringMode.DEDUCTION, "", "{}"))
                .isEqualByComparingTo("0");
    }

    @Test
    void deduction_ignoresConfigScore() {
        // 直接取值, 与 cfg 无关
        assertThat(ev.scoreItem(ScoringMode.DEDUCTION, "-5", "{\"score\":\"2\"}"))
                .isEqualByComparingTo("-5");
    }

    // ==================== ADDITION (responseValue=检查员直接拨的加分值) ====================
    // 前端 handleAdditionSelect: getAdditionRange.max, step 钳到 [0,max], responseValue 为正数.

    @Test
    void addition_takesResponseValueDirectlyAsPositive() {
        assertThat(ev.scoreItem(ScoringMode.ADDITION, "2", "{}"))
                .isEqualByComparingTo("2");
    }

    @Test
    void addition_negativeResponseMadePositive() {
        assertThat(ev.scoreItem(ScoringMode.ADDITION, "-2", "{}"))
                .isEqualByComparingTo("2");
    }

    @Test
    void addition_emptyResponseIsZero() {
        assertThat(ev.scoreItem(ScoringMode.ADDITION, "", "{}"))
                .isEqualByComparingTo("0");
    }

    // ==================== CUMULATIVE (responseValue=次数, scorePerUnit*次数) ====================

    @Test
    void cumulative_multipliesScorePerUnitByCount() {
        assertThat(ev.scoreItem(ScoringMode.CUMULATIVE, "5", "{\"scorePerUnit\":\"1\"}"))
                .isEqualByComparingTo("5");
    }

    @Test
    void cumulative_supportsScorePerCountFieldName() {
        // ItemEditor.vue 实际序列化为 scorePerCount
        assertThat(ev.scoreItem(ScoringMode.CUMULATIVE, "3", "{\"scorePerCount\":\"-2\"}"))
                .isEqualByComparingTo("-6");
    }

    @Test
    void cumulative_emptyResponseIsZero() {
        assertThat(ev.scoreItem(ScoringMode.CUMULATIVE, "", "{\"scorePerUnit\":\"1\"}"))
                .isEqualByComparingTo("0");
    }

    // ==================== DIRECT (responseValue 即分) ====================

    @Test
    void direct_responseValueIsTheScore() {
        assertThat(ev.scoreItem(ScoringMode.DIRECT, "88", "{}"))
                .isEqualByComparingTo("88");
    }

    @Test
    void direct_emptyResponseIsZero() {
        assertThat(ev.scoreItem(ScoringMode.DIRECT, "", "{}"))
                .isEqualByComparingTo("0");
    }

    @Test
    void direct_nonNumericIsZero() {
        assertThat(ev.scoreItem(ScoringMode.DIRECT, "abc", "{}"))
                .isEqualByComparingTo("0");
    }

    // ==================== PASS_FAIL ====================

    @Test
    void passFail_failUsesFailScore() {
        assertThat(ev.scoreItem(ScoringMode.PASS_FAIL, "FAIL", "{\"passScore\":\"0\",\"failScore\":\"-5\"}"))
                .isEqualByComparingTo("-5");
    }

    @Test
    void passFail_passUsesPassScore() {
        assertThat(ev.scoreItem(ScoringMode.PASS_FAIL, "PASS", "{\"passScore\":\"2\",\"failScore\":\"-5\"}"))
                .isEqualByComparingTo("2");
    }

    @Test
    void passFail_defaultsPassZeroFailMinusFive() {
        assertThat(ev.scoreItem(ScoringMode.PASS_FAIL, "PASS", "{}"))
                .isEqualByComparingTo("0");
        assertThat(ev.scoreItem(ScoringMode.PASS_FAIL, "FAIL", "{}"))
                .isEqualByComparingTo("-5");
    }

    @Test
    void passFail_emptyResponseIsZero() {
        assertThat(ev.scoreItem(ScoringMode.PASS_FAIL, "", "{}"))
                .isEqualByComparingTo("0");
    }

    // ==================== LEVEL (responseValue=等级 label) ====================

    @Test
    void level_looksUpScoreByLabel() {
        String cfg = "{\"levels\":[{\"label\":\"优\",\"score\":\"10\"},{\"label\":\"良\",\"score\":\"8\"}]}";
        assertThat(ev.scoreItem(ScoringMode.LEVEL, "良", cfg))
                .isEqualByComparingTo("8");
    }

    @Test
    void level_unknownLabelIsZero() {
        String cfg = "{\"levels\":[{\"label\":\"优\",\"score\":\"10\"}]}";
        assertThat(ev.scoreItem(ScoringMode.LEVEL, "差", cfg))
                .isEqualByComparingTo("0");
    }

    @Test
    void level_emptyResponseIsZero() {
        String cfg = "{\"levels\":[{\"label\":\"优\",\"score\":\"10\"}]}";
        assertThat(ev.scoreItem(ScoringMode.LEVEL, "", cfg))
                .isEqualByComparingTo("0");
    }

    @Test
    void level_numericScoreInJson() {
        // 数值可能是 JSON number 而非 string
        String cfg = "{\"levels\":[{\"label\":\"良\",\"score\":8}]}";
        assertThat(ev.scoreItem(ScoringMode.LEVEL, "良", cfg))
                .isEqualByComparingTo("8");
    }

    // ==================== SCORE_TABLE (responseValue=label, options 数组) ====================

    @Test
    void scoreTable_looksUpScoreByLabelInOptions() {
        String cfg = "{\"options\":[{\"label\":\"优秀\",\"score\":\"10\"},{\"label\":\"一般\",\"score\":\"3\"}]}";
        assertThat(ev.scoreItem(ScoringMode.SCORE_TABLE, "一般", cfg))
                .isEqualByComparingTo("3");
    }

    @Test
    void scoreTable_unknownLabelIsZero() {
        String cfg = "{\"options\":[{\"label\":\"优秀\",\"score\":\"10\"}]}";
        assertThat(ev.scoreItem(ScoringMode.SCORE_TABLE, "缺这个", cfg))
                .isEqualByComparingTo("0");
    }

    @Test
    void scoreTable_supportsLevelsFieldNameToo() {
        String cfg = "{\"levels\":[{\"label\":\"甲\",\"score\":\"7\"}]}";
        assertThat(ev.scoreItem(ScoringMode.SCORE_TABLE, "甲", cfg))
                .isEqualByComparingTo("7");
    }

    // ==================== RATING_SCALE (responseValue=星数) ====================

    @Test
    void ratingScale_proportionalToMaxScore() {
        assertThat(ev.scoreItem(ScoringMode.RATING_SCALE, "4", "{\"maxStars\":\"5\",\"maxScore\":\"100\"}"))
                .isEqualByComparingTo("80");
    }

    @Test
    void ratingScale_halfUpRounding() {
        // 3/7*100 = 42.857... → HALF_UP → 43
        assertThat(ev.scoreItem(ScoringMode.RATING_SCALE, "3", "{\"maxStars\":\"7\",\"maxScore\":\"100\"}"))
                .isEqualByComparingTo("43");
    }

    @Test
    void ratingScale_defaultMaxStarsFiveMaxScoreHundred() {
        assertThat(ev.scoreItem(ScoringMode.RATING_SCALE, "5", "{}"))
                .isEqualByComparingTo("100");
    }

    @Test
    void ratingScale_maxRatingFallback() {
        assertThat(ev.scoreItem(ScoringMode.RATING_SCALE, "2", "{\"maxRating\":\"4\",\"maxScore\":\"100\"}"))
                .isEqualByComparingTo("50");
    }

    @Test
    void ratingScale_emptyResponseIsZero() {
        assertThat(ev.scoreItem(ScoringMode.RATING_SCALE, "", "{\"maxStars\":\"5\",\"maxScore\":\"100\"}"))
                .isEqualByComparingTo("0");
    }

    // ==================== TIERED_DEDUCTION (responseValue=档位 label, tiers 数组查分) ====================
    // 前端 ItemEditor serializeScoringConfig: obj.tiers = [{label, score}], score 已被校验钳到 <= 0
    // (Math.min(0, t.score)). 故按 label 查到 score 原样返回 (已是负数或 0).

    @Test
    void tieredDeduction_looksUpScoreByLabel() {
        String cfg = "{\"tiers\":[{\"label\":\"轻微\",\"score\":-1},{\"label\":\"一般\",\"score\":-3},{\"label\":\"严重\",\"score\":-5}]}";
        assertThat(ev.scoreItem(ScoringMode.TIERED_DEDUCTION, "一般", cfg))
                .isEqualByComparingTo("-3");
    }

    @Test
    void tieredDeduction_unknownLabelIsZero() {
        String cfg = "{\"tiers\":[{\"label\":\"轻微\",\"score\":-1}]}";
        assertThat(ev.scoreItem(ScoringMode.TIERED_DEDUCTION, "不存在", cfg))
                .isEqualByComparingTo("0");
    }

    @Test
    void tieredDeduction_emptyResponseIsZero() {
        String cfg = "{\"tiers\":[{\"label\":\"轻微\",\"score\":-1}]}";
        assertThat(ev.scoreItem(ScoringMode.TIERED_DEDUCTION, "", cfg))
                .isEqualByComparingTo("0");
    }

    @Test
    void tieredDeduction_stringScoreParsed() {
        String cfg = "{\"tiers\":[{\"label\":\"重大\",\"score\":\"-10\"}]}";
        assertThat(ev.scoreItem(ScoringMode.TIERED_DEDUCTION, "重大", cfg))
                .isEqualByComparingTo("-10");
    }

    // ==================== WEIGHTED_MULTI (复用 SeverityNormalizer, severity→得分语义/正分) ====================
    // severity ∈ [0,1] (0=完美, 1=最差). P2.1 已定: WEIGHTED_MULTI 用得分语义,
    // score = round((1 - severity) × maxScore) — 越好得分越高, 完美拿满分, 最差得 0.

    @Test
    void weightedMulti_perfectIsFullScore() {
        String cfg = "{\"dimensions\":[" +
                "{\"key\":\"hygiene\",\"weight\":0.5,\"mode\":\"LEVEL\"}," +
                "{\"key\":\"safety\",\"weight\":0.5,\"mode\":\"PASS_FAIL\"}" +
                "],\"anyDimensionAbove\":0.9,\"maxScore\":10}";
        // hygiene=A(sev 0) + safety=PASS(sev 0) → severity 0 → (1-0)×10 = 10 (满分)
        assertThat(ev.scoreItem(ScoringMode.WEIGHTED_MULTI,
                "{\"hygiene\":\"A\",\"safety\":\"PASS\"}", cfg))
                .isEqualByComparingTo("10");
    }

    @Test
    void weightedMulti_worstSeverityIsZeroScore() {
        String cfg = "{\"dimensions\":[" +
                "{\"key\":\"hygiene\",\"weight\":0.5,\"mode\":\"LEVEL\"}," +
                "{\"key\":\"safety\",\"weight\":0.5,\"mode\":\"PASS_FAIL\"}" +
                "],\"anyDimensionAbove\":0.9,\"maxScore\":10}";
        // safety=FAIL(1.0) >= 0.9 → 升级 max=1.0 → (1-1.0)×10 = 0 (最低分)
        assertThat(ev.scoreItem(ScoringMode.WEIGHTED_MULTI,
                "{\"hygiene\":\"A\",\"safety\":\"FAIL\"}", cfg))
                .isEqualByComparingTo("0");
    }

    @Test
    void weightedMulti_emptyResponseIsZero() {
        String cfg = "{\"dimensions\":[{\"key\":\"x\",\"weight\":1,\"mode\":\"PASS_FAIL\"}],\"maxScore\":10}";
        assertThat(ev.scoreItem(ScoringMode.WEIGHTED_MULTI, "{}", cfg))
                .isEqualByComparingTo("0");
    }

    // ==================== RISK_MATRIX (复用 RiskMatrixNormalizer, severity→扣分) ====================

    private static final String RISK_CFG = "{\"matrix\":[" +
            "[{\"level\":\"L\"},{\"level\":\"L\"},{\"level\":\"M\"},{\"level\":\"H\"}]," +
            "[{\"level\":\"L\"},{\"level\":\"M\"},{\"level\":\"M\"},{\"level\":\"H\"}]," +
            "[{\"level\":\"M\"},{\"level\":\"M\"},{\"level\":\"H\"},{\"level\":\"VH\"}]," +
            "[{\"level\":\"M\"},{\"level\":\"H\"},{\"level\":\"VH\"},{\"level\":\"VH\"}]" +
            "],\"levelToSeverity\":{\"L\":0.0,\"M\":0.4,\"H\":0.75,\"VH\":1.0},\"maxScore\":20}";

    @Test
    void riskMatrix_lowCellIsZeroDeduction() {
        // 0,0 → L → severity 0 → 扣 0
        assertThat(ev.scoreItem(ScoringMode.RISK_MATRIX, "0,0", RISK_CFG))
                .isEqualByComparingTo("0");
    }

    @Test
    void riskMatrix_highCellFullDeduction() {
        // 3,3 → VH → severity 1.0 → -(1.0 × 20) = -20
        assertThat(ev.scoreItem(ScoringMode.RISK_MATRIX, "3,3", RISK_CFG))
                .isEqualByComparingTo("-20");
    }

    @Test
    void riskMatrix_midCellScaled() {
        // 1,1 → M → severity 0.4 → -(0.4 × 20) = -8
        assertThat(ev.scoreItem(ScoringMode.RISK_MATRIX, "1,1", RISK_CFG))
                .isEqualByComparingTo("-8");
    }

    @Test
    void riskMatrix_outOfBoundsIsZero() {
        assertThat(ev.scoreItem(ScoringMode.RISK_MATRIX, "9,9", RISK_CFG))
                .isEqualByComparingTo("0");
    }

    @Test
    void riskMatrix_defaultMaxScoreWhenAbsent() {
        String cfg = RISK_CFG.replace(",\"maxScore\":20", "");
        // 默认 maxScore=100 → 3,3 VH → -(1.0 × 100) = -100
        assertThat(ev.scoreItem(ScoringMode.RISK_MATRIX, "3,3", cfg))
                .isEqualByComparingTo("-100");
    }

    // ==================== THRESHOLD (responseValue=实测值, 落阈值档→分) ====================
    // cfg.thresholds = [{upTo, score}] 升序; 实测值 <= upTo 落该档. 无 upTo 的视为兜底档.

    @Test
    void threshold_fallsIntoTierByMeasuredValue() {
        String cfg = "{\"thresholds\":[{\"upTo\":10,\"score\":0},{\"upTo\":20,\"score\":-3},{\"upTo\":50,\"score\":-8}]}";
        // 实测 15 → <=20 档 → -3
        assertThat(ev.scoreItem(ScoringMode.THRESHOLD, "15", cfg))
                .isEqualByComparingTo("-3");
    }

    @Test
    void threshold_lowestTier() {
        String cfg = "{\"thresholds\":[{\"upTo\":10,\"score\":0},{\"upTo\":20,\"score\":-3}]}";
        assertThat(ev.scoreItem(ScoringMode.THRESHOLD, "5", cfg))
                .isEqualByComparingTo("0");
    }

    @Test
    void threshold_overTopTierUsesCatchAll() {
        // 超过所有 upTo, 用无 upTo 的兜底档
        String cfg = "{\"thresholds\":[{\"upTo\":10,\"score\":0},{\"score\":-10}]}";
        assertThat(ev.scoreItem(ScoringMode.THRESHOLD, "99", cfg))
                .isEqualByComparingTo("-10");
    }

    @Test
    void threshold_nonNumericIsZero() {
        String cfg = "{\"thresholds\":[{\"upTo\":10,\"score\":0}]}";
        assertThat(ev.scoreItem(ScoringMode.THRESHOLD, "abc", cfg))
                .isEqualByComparingTo("0");
    }

    @Test
    void threshold_missingConfigIsZero() {
        assertThat(ev.scoreItem(ScoringMode.THRESHOLD, "15", "{}"))
                .isEqualByComparingTo("0");
    }

    // ==================== FORMULA (复用 FormulaEvaluator) ====================
    // cfg.formula = JS 表达式; 变量 value=responseValue 数值化. 缺公式/异常 → 0.

    @Test
    void formula_evaluatesExpressionWithValueVariable() {
        // value=4 → value*value = 16
        assertThat(evWithFormula.scoreItem(ScoringMode.FORMULA, "4", "{\"formula\":\"value * value\"}"))
                .isEqualByComparingTo("16");
    }

    @Test
    void formula_clampsToMinMax() {
        // value=100, maxScore=10 → min(10, value) = 10
        assertThat(evWithFormula.scoreItem(ScoringMode.FORMULA, "100",
                "{\"formula\":\"value\",\"maxScore\":10,\"minScore\":0}"))
                .isEqualByComparingTo("10");
    }

    @Test
    void formula_missingFormulaIsZero() {
        assertThat(evWithFormula.scoreItem(ScoringMode.FORMULA, "5", "{}"))
                .isEqualByComparingTo("0");
    }

    @Test
    void formula_badExpressionIsZero() {
        assertThat(evWithFormula.scoreItem(ScoringMode.FORMULA, "5", "{\"formula\":\"this is not js ((\"}"))
                .isEqualByComparingTo("0");
    }

    @Test
    void formula_withoutEvaluatorIsZero() {
        // 没注入 FormulaEvaluator (无参构造) → FORMULA 退化为 0, 不崩
        assertThat(ev.scoreItem(ScoringMode.FORMULA, "4", "{\"formula\":\"value * value\"}"))
                .isEqualByComparingTo("0");
    }

    @Test
    void complexModesEmptyConfigIsZero() {
        assertThat(ev.scoreItem(ScoringMode.WEIGHTED_MULTI, "x", "{}")).isEqualByComparingTo("0");
        assertThat(ev.scoreItem(ScoringMode.RISK_MATRIX, "x", "{}")).isEqualByComparingTo("0");
        assertThat(ev.scoreItem(ScoringMode.THRESHOLD, "x", "{}")).isEqualByComparingTo("0");
        assertThat(ev.scoreItem(ScoringMode.FORMULA, "x", "{}")).isEqualByComparingTo("0");
        assertThat(ev.scoreItem(ScoringMode.TIERED_DEDUCTION, "x", "{}")).isEqualByComparingTo("0");
    }

    @Test
    void nullModeIsZero() {
        assertThat(ev.scoreItem(null, "5", "{}")).isEqualByComparingTo("0");
    }
}
