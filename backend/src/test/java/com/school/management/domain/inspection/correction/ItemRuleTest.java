package com.school.management.domain.inspection.correction;

import com.school.management.domain.inspection.model.execution.ScoringMode;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V20260524_7 ItemRule 检查项级覆盖单测 (重构后).
 * <p>新模型: criticality / neverCorrect / baseSeverityMap / deadlineOverrideDays.
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
        void parse_new_schema() {
            String json = "{\"criticality\":\"RED\",\"neverCorrect\":false," +
                    "\"baseSeverityMap\":{\"FAIL\":\"HIGH\",\"D\":\"HIGH\",\"C\":\"MEDIUM\"}," +
                    "\"deadlineOverrideDays\":2}";
            ItemRule r = ItemRule.fromJson(json);
            assertTrue(r.isRedLine());
            assertEquals(Severity.HIGH, r.lookupBaseSeverity("FAIL"));
            assertEquals(Severity.HIGH, r.lookupBaseSeverity("d"));   // case-insensitive
            assertEquals(Severity.MEDIUM, r.lookupBaseSeverity("C"));
            assertNull(r.lookupBaseSeverity("PASS"));
            assertEquals(Integer.valueOf(2), r.getDeadlineOverrideDays());
        }

        @Test
        void parse_legacy_forceCorrect_maps_to_HIGH() {
            String json = "{\"forceCorrect\":[\"FAIL\",\"D\"]," +
                    "\"deadlineOverride\":{\"high\":1,\"medium\":3,\"low\":5}}";
            ItemRule r = ItemRule.fromJson(json);
            assertEquals(Severity.HIGH, r.lookupBaseSeverity("FAIL"));
            assertEquals(Severity.HIGH, r.lookupBaseSeverity("D"));
            // 旧 deadlineOverride.high → 取 high
            assertEquals(Integer.valueOf(1), r.getDeadlineOverrideDays());
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
        void baseSeverityMap_explicit_response_to_HIGH() {
            // 配置 baseSeverityMap 把 LEVEL=B 映射到 HIGH (B 默认是 NONE)
            ItemRule rule = ItemRule.fromJson("{\"baseSeverityMap\":{\"B\":\"HIGH\"}}");
            SubmissionDetail d = det(ScoringMode.LEVEL, "B", null, null);
            CorrectionVerdict v = engine.judge(d, normal, rule, 0);
            assertEquals(Severity.HIGH, v.getSeverity());
        }

        @Test
        void red_line_forces_HIGH_on_FAIL() {
            // 即使 LEVEL=B 默认 NONE, RED 红线 + baseSeverityMap=MEDIUM 也会被升 HIGH
            ItemRule rule = ItemRule.fromJson(
                    "{\"criticality\":\"RED\",\"baseSeverityMap\":{\"B\":\"MEDIUM\"}}");
            SubmissionDetail d = det(ScoringMode.LEVEL, "B", null, null);
            CorrectionVerdict v = engine.judge(d, normal, rule, 0);
            assertEquals(Severity.HIGH, v.getSeverity());
        }

        @Test
        void deadlineOverrideDays_used() {
            ItemRule rule = ItemRule.fromJson("{\"deadlineOverrideDays\":1}");
            SubmissionDetail d = det(ScoringMode.PASS_FAIL, "FAIL", null, null);
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
