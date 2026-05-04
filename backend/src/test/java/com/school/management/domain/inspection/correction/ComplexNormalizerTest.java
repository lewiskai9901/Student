package com.school.management.domain.inspection.correction;

import com.school.management.domain.inspection.model.execution.ScoringMode;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint 2: RISK_MATRIX / WEIGHTED_MULTI / TIERED_DEDUCTION normalizer 单测.
 */
class ComplexNormalizerTest {

    private SubmissionDetail det(ScoringMode mode, String resp, String cfg, BigDecimal score) {
        return SubmissionDetail.builder()
                .id(1L).itemCode("X").itemName("test")
                .scoringMode(mode)
                .responseValue(resp)
                .score(score)
                .scoringConfig(cfg)
                .build();
    }

    @Nested
    @DisplayName("RISK_MATRIX")
    class RiskMatrix {
        private final String CFG = "{\"matrix\":[" +
                "[{\"level\":\"L\"},{\"level\":\"L\"},{\"level\":\"M\"},{\"level\":\"H\"}]," +
                "[{\"level\":\"L\"},{\"level\":\"M\"},{\"level\":\"M\"},{\"level\":\"H\"}]," +
                "[{\"level\":\"M\"},{\"level\":\"M\"},{\"level\":\"H\"},{\"level\":\"VH\"}]," +
                "[{\"level\":\"M\"},{\"level\":\"H\"},{\"level\":\"VH\"},{\"level\":\"VH\"}]" +
                "],\"levelToSeverity\":{\"L\":0.0,\"M\":0.4,\"H\":0.75,\"VH\":1.0}}";

        @Test
        void low_left_top() {
            SubmissionDetail d = det(ScoringMode.RISK_MATRIX, "0,0", CFG, null);
            Double sev = SeverityNormalizer.of(ScoringMode.RISK_MATRIX).normalize(d, null);
            assertEquals(0.0, sev, 0.001);
        }

        @Test
        void high_right_bottom() {
            SubmissionDetail d = det(ScoringMode.RISK_MATRIX, "3,3", CFG, null);
            Double sev = SeverityNormalizer.of(ScoringMode.RISK_MATRIX).normalize(d, null);
            assertEquals(1.0, sev, 0.001);  // VH
        }

        @Test
        void mid_cell_M() {
            SubmissionDetail d = det(ScoringMode.RISK_MATRIX, "1,1", CFG, null);
            Double sev = SeverityNormalizer.of(ScoringMode.RISK_MATRIX).normalize(d, null);
            assertEquals(0.4, sev, 0.001);  // M
        }

        @Test
        void json_format_response() {
            SubmissionDetail d = det(ScoringMode.RISK_MATRIX,
                    "{\"probability\":3,\"impact\":2}", CFG, null);
            Double sev = SeverityNormalizer.of(ScoringMode.RISK_MATRIX).normalize(d, null);
            assertEquals(1.0, sev, 0.001);  // VH
        }

        @Test
        void out_of_bounds_returns_null() {
            SubmissionDetail d = det(ScoringMode.RISK_MATRIX, "9,9", CFG, null);
            Double sev = SeverityNormalizer.of(ScoringMode.RISK_MATRIX).normalize(d, null);
            assertNull(sev);
        }

        @Test
        void no_cfg_returns_null() {
            SubmissionDetail d = det(ScoringMode.RISK_MATRIX, "1,2", null, null);
            Double sev = SeverityNormalizer.of(ScoringMode.RISK_MATRIX).normalize(d, null);
            assertNull(sev);
        }

        @Test
        void engine_classifies_VH_to_HIGH() {
            CorrectionEngine engine = new CorrectionEngine();
            SubmissionDetail d = det(ScoringMode.RISK_MATRIX, "3,3", CFG, null);
            CorrectionVerdict v = engine.judge(d, ProjectCorrectivePolicy.normalDefault(), 0);
            assertEquals(Severity.HIGH, v.getSeverity());
            assertTrue(v.isMustCorrect());
        }
    }

    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("WEIGHTED_MULTI")
    class WeightedMulti {
        private final String CFG = "{\"dimensions\":[" +
                "{\"key\":\"hygiene\",\"weight\":0.5,\"mode\":\"LEVEL\"}," +
                "{\"key\":\"safety\",\"weight\":0.5,\"mode\":\"PASS_FAIL\"}" +
                "],\"anyDimensionAbove\":0.9}";

        @Test
        void all_perfect_NONE() {
            SubmissionDetail d = det(ScoringMode.WEIGHTED_MULTI,
                    "{\"hygiene\":\"A\",\"safety\":\"PASS\"}", CFG, null);
            Double sev = SeverityNormalizer.of(ScoringMode.WEIGHTED_MULTI).normalize(d, null);
            assertEquals(0.0, sev, 0.001);
        }

        @Test
        void mixed_C_PASS_weighted() {
            // hygiene=C(0.6) × 0.5 + safety=PASS(0) × 0.5 = 0.3
            SubmissionDetail d = det(ScoringMode.WEIGHTED_MULTI,
                    "{\"hygiene\":\"C\",\"safety\":\"PASS\"}", CFG, null);
            Double sev = SeverityNormalizer.of(ScoringMode.WEIGHTED_MULTI).normalize(d, null);
            assertEquals(0.3, sev, 0.001);
        }

        @Test
        void any_above_escalates_to_max() {
            // safety=FAIL(1.0) >= 0.9 → 触发升级 → max=1.0 (而非加权 0.5)
            SubmissionDetail d = det(ScoringMode.WEIGHTED_MULTI,
                    "{\"hygiene\":\"A\",\"safety\":\"FAIL\"}", CFG, null);
            Double sev = SeverityNormalizer.of(ScoringMode.WEIGHTED_MULTI).normalize(d, null);
            assertEquals(1.0, sev, 0.001);
        }

        @Test
        void empty_response_returns_null() {
            SubmissionDetail d = det(ScoringMode.WEIGHTED_MULTI, "{}", CFG, null);
            Double sev = SeverityNormalizer.of(ScoringMode.WEIGHTED_MULTI).normalize(d, null);
            assertNull(sev);
        }
    }

    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("TIERED_DEDUCTION")
    class Tiered {
        private final String CFG = "{\"tiers\":[" +
                "{\"upTo\":2,\"label\":\"轻\"}," +
                "{\"upTo\":5,\"label\":\"中\"}," +
                "{\"upTo\":10,\"label\":\"重\"}]}";

        @Test
        void max_tier_full_deduction() {
            // -10/10 = 1.0
            SubmissionDetail d = det(ScoringMode.TIERED_DEDUCTION,
                    null, CFG, new BigDecimal("-10"));
            Double sev = SeverityNormalizer.of(ScoringMode.TIERED_DEDUCTION).normalize(d, null);
            assertEquals(1.0, sev, 0.001);
        }

        @Test
        void mid_tier() {
            // -5/10 = 0.5
            SubmissionDetail d = det(ScoringMode.TIERED_DEDUCTION,
                    null, CFG, new BigDecimal("-5"));
            Double sev = SeverityNormalizer.of(ScoringMode.TIERED_DEDUCTION).normalize(d, null);
            assertEquals(0.5, sev, 0.001);
        }

        @Test
        void zero_NONE() {
            SubmissionDetail d = det(ScoringMode.TIERED_DEDUCTION, null, CFG, BigDecimal.ZERO);
            Double sev = SeverityNormalizer.of(ScoringMode.TIERED_DEDUCTION).normalize(d, null);
            assertEquals(0.0, sev, 0.001);
        }

        @Test
        void no_cfg_falls_back_to_DEDUCTION() {
            SubmissionDetail d = det(ScoringMode.TIERED_DEDUCTION, null, null, new BigDecimal("-3"));
            // 退回到 DeductionNormalizer (用 itemWeight 或默认 10)
            d = SubmissionDetail.builder()
                    .scoringMode(ScoringMode.TIERED_DEDUCTION)
                    .score(new BigDecimal("-3"))
                    .itemWeight(new BigDecimal("10"))
                    .build();
            Double sev = SeverityNormalizer.of(ScoringMode.TIERED_DEDUCTION).normalize(d, null);
            assertEquals(0.3, sev, 0.001);
        }
    }
}
