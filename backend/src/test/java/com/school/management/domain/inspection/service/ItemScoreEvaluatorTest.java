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

    // ==================== DEDUCTION (responseValue=数量, 结果取负) ====================

    @Test
    void deduction_multipliesAbsConfigScoreByQuantityAndNegates() {
        assertThat(ev.scoreItem(ScoringMode.DEDUCTION, "3", "{\"score\":\"2\"}"))
                .isEqualByComparingTo("-6");
    }

    @Test
    void deduction_usesConfigScoreFallback() {
        assertThat(ev.scoreItem(ScoringMode.DEDUCTION, "2", "{\"configScore\":\"5\"}"))
                .isEqualByComparingTo("-10");
    }

    @Test
    void deduction_emptyResponseIsZero() {
        assertThat(ev.scoreItem(ScoringMode.DEDUCTION, "", "{\"score\":\"2\"}"))
                .isEqualByComparingTo("0");
    }

    @Test
    void deduction_missingConfigIsZero() {
        assertThat(ev.scoreItem(ScoringMode.DEDUCTION, "3", null))
                .isEqualByComparingTo("0");
    }

    // ==================== ADDITION (responseValue=数量) ====================

    @Test
    void addition_multipliesAbsConfigScoreByQuantity() {
        assertThat(ev.scoreItem(ScoringMode.ADDITION, "2", "{\"score\":\"2\"}"))
                .isEqualByComparingTo("4");
    }

    @Test
    void addition_emptyResponseIsZero() {
        assertThat(ev.scoreItem(ScoringMode.ADDITION, "", "{\"score\":\"2\"}"))
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

    // ==================== 复杂模式占位 (本任务返回 0) ====================

    @Test
    void complexModesReturnZeroForNow() {
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
