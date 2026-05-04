package com.school.management.domain.inspection.correction;

import com.school.management.domain.inspection.model.execution.ScoringMode;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint 3: ItemRule 检查项级覆盖单测.
 */
class ItemRuleTest {

    private final CorrectionEngine engine = new CorrectionEngine();
    private final ProjectCorrectivePolicy normal = ProjectCorrectivePolicy.normalDefault();

    private SubmissionDetail det(ScoringMode m, String resp, BigDecimal score, BigDecimal weight) {
        return SubmissionDetail.builder()
                .id(1L).itemCode("X").itemName("test")
                .scoringMode(m).responseValue(resp).score(score).itemWeight(weight)
                .build();
    }

    @Nested
    @DisplayName("ItemRule.fromJson 解析")
    class Parse {
        @Test
        void empty_json() {
            assertSame(ItemRule.EMPTY, ItemRule.fromJson(null));
            assertSame(ItemRule.EMPTY, ItemRule.fromJson(""));
            assertSame(ItemRule.EMPTY, ItemRule.fromJson("   "));
        }

        @Test
        void parse_full() {
            String json = "{\"neverCorrect\":true,\"forceCorrect\":[\"FAIL\",\"D\"]," +
                    "\"thresholdOverride\":{\"high\":0.6,\"medium\":0.4,\"low\":0.2}," +
                    "\"deadlineOverride\":{\"high\":1,\"medium\":3,\"low\":5}}";
            ItemRule r = ItemRule.fromJson(json);
            assertTrue(r.isNeverCorrect());
            assertTrue(r.isForceCorrect("FAIL"));
            assertTrue(r.isForceCorrect("d"));
            assertFalse(r.isForceCorrect("PASS"));
            assertEquals(0.6, r.getThresholdOverride().high(), 0.001);
            assertEquals(1, r.getDeadlineOverride().high());
        }

        @Test
        void invalid_json_returns_EMPTY() {
            assertSame(ItemRule.EMPTY, ItemRule.fromJson("not json"));
        }
    }

    @Nested
    @DisplayName("Engine + ItemRule 集成")
    class EngineWithRule {
        @Test
        void neverCorrect_skips_even_FAIL() {
            ItemRule rule = ItemRule.fromJson("{\"neverCorrect\":true}");
            SubmissionDetail d = det(ScoringMode.PASS_FAIL, "FAIL", null, null);
            CorrectionVerdict v = engine.judge(d, normal, rule, 0);
            assertEquals(Severity.NONE, v.getSeverity());
            assertFalse(v.shouldSuggest());
        }

        @Test
        void forceCorrect_makes_HIGH_with_must() {
            // 配置 forceCorrect 在 LEVEL=B 时强制建单 (B 默认是 NONE)
            ItemRule rule = ItemRule.fromJson("{\"forceCorrect\":[\"B\"]}");
            SubmissionDetail d = det(ScoringMode.LEVEL, "B", null, null);
            CorrectionVerdict v = engine.judge(d, normal, rule, 0);
            assertEquals(Severity.HIGH, v.getSeverity());
            assertTrue(v.isMustCorrect());
        }

        @Test
        void thresholdOverride_lowers_severity() {
            // -3/10 = 0.3, NORMAL.low=0.3 → LOW. itemRule low=0.5 → NONE.
            ItemRule rule = ItemRule.fromJson(
                    "{\"thresholdOverride\":{\"high\":0.9,\"medium\":0.7,\"low\":0.5}}");
            SubmissionDetail d = det(ScoringMode.DEDUCTION, null,
                    new BigDecimal("-3"), new BigDecimal("10"));
            CorrectionVerdict v = engine.judge(d, normal, rule, 0);
            assertEquals(Severity.NONE, v.getSeverity());
        }

        @Test
        void thresholdOverride_raises_severity() {
            // -3/10 = 0.3, NORMAL → LOW. itemRule m=0.3, h=0.5 → MEDIUM.
            ItemRule rule = ItemRule.fromJson(
                    "{\"thresholdOverride\":{\"high\":0.5,\"medium\":0.3,\"low\":0.1}}");
            SubmissionDetail d = det(ScoringMode.DEDUCTION, null,
                    new BigDecimal("-3"), new BigDecimal("10"));
            CorrectionVerdict v = engine.judge(d, normal, rule, 0);
            assertEquals(Severity.MEDIUM, v.getSeverity());
        }

        @Test
        void deadlineOverride_used() {
            ItemRule rule = ItemRule.fromJson(
                    "{\"deadlineOverride\":{\"high\":1,\"medium\":2,\"low\":3}}");
            SubmissionDetail d = det(ScoringMode.PASS_FAIL, "FAIL", null, null);
            CorrectionVerdict v = engine.judge(d, normal, rule, 0);
            assertEquals(Severity.HIGH, v.getSeverity());
            assertEquals(1, v.getSuggestedDeadlineDays());
        }

        @Test
        void forceCorrect_with_deadlineOverride_combines() {
            ItemRule rule = ItemRule.fromJson(
                    "{\"forceCorrect\":[\"B\"],\"deadlineOverride\":{\"high\":1}}");
            SubmissionDetail d = det(ScoringMode.LEVEL, "B", null, null);
            CorrectionVerdict v = engine.judge(d, normal, rule, 0);
            assertEquals(Severity.HIGH, v.getSeverity());
            assertEquals(1, v.getSuggestedDeadlineDays());
        }

        @Test
        void EMPTY_rule_acts_like_no_override() {
            SubmissionDetail d = det(ScoringMode.PASS_FAIL, "FAIL", null, null);
            CorrectionVerdict v1 = engine.judge(d, normal, 0);
            CorrectionVerdict v2 = engine.judge(d, normal, ItemRule.EMPTY, 0);
            assertEquals(v1.getSeverity(), v2.getSeverity());
            assertEquals(v1.getSuggestedDeadlineDays(), v2.getSuggestedDeadlineDays());
        }
    }
}
