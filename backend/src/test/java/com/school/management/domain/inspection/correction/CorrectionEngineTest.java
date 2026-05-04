package com.school.management.domain.inspection.correction;

import com.school.management.domain.inspection.model.execution.ScoringMode;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 整改判定引擎 — 单元测试 (Sprint 1).
 *
 * <p>覆盖 5 个 mode normalizer + threshold 分级 + 复发增强 + OFF 短路.
 */
class CorrectionEngineTest {

    private final CorrectionEngine engine = new CorrectionEngine();
    private final ProjectCorrectivePolicy normal = ProjectCorrectivePolicy.normalDefault();

    private SubmissionDetail detail(ScoringMode mode, String response, BigDecimal score, BigDecimal weight) {
        return SubmissionDetail.builder()
                .id(1L)
                .itemCode("VHS-01")
                .itemName("卫生检查")
                .scoringMode(mode)
                .responseValue(response)
                .score(score)
                .itemWeight(weight)
                .build();
    }

    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PASS_FAIL")
    class PassFail {
        @Test
        void fail_triggers_HIGH() {
            SubmissionDetail d = detail(ScoringMode.PASS_FAIL, "FAIL", null, null);
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.HIGH, v.getSeverity());
            assertTrue(v.isMustCorrect());
            assertEquals(1.0, v.getSeverityScore(), 0.0001);
        }

        @Test
        void pass_triggers_NONE() {
            SubmissionDetail d = detail(ScoringMode.PASS_FAIL, "PASS", null, null);
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.NONE, v.getSeverity());
            assertFalse(v.shouldSuggest());
        }

        @Test
        void na_triggers_NONE() {
            SubmissionDetail d = detail(ScoringMode.PASS_FAIL, "N/A", null, null);
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.NONE, v.getSeverity());
        }
    }

    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DEDUCTION")
    class Deduction {
        @Test
        void full_deduction_HIGH() {
            // 满扣: 扣 10/10 → sev=1.0 → HIGH
            SubmissionDetail d = detail(ScoringMode.DEDUCTION, null,
                    new BigDecimal("-10"), new BigDecimal("10"));
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.HIGH, v.getSeverity());
            assertEquals(1.0, v.getSeverityScore(), 0.0001);
        }

        @Test
        void half_deduction_MEDIUM_under_normal() {
            // 半扣: 扣 5/10 → sev=0.5 → MEDIUM (normal: m=0.5)
            SubmissionDetail d = detail(ScoringMode.DEDUCTION, null,
                    new BigDecimal("-5"), new BigDecimal("10"));
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.MEDIUM, v.getSeverity());
        }

        @Test
        void light_deduction_LOW() {
            // 轻扣: 扣 3/10 → sev=0.3 → LOW (normal: l=0.3)
            SubmissionDetail d = detail(ScoringMode.DEDUCTION, null,
                    new BigDecimal("-3"), new BigDecimal("10"));
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.LOW, v.getSeverity());
        }

        @Test
        void no_deduction_NONE() {
            SubmissionDetail d = detail(ScoringMode.DEDUCTION, null,
                    BigDecimal.ZERO, new BigDecimal("10"));
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.NONE, v.getSeverity());
        }
    }

    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("LEVEL")
    class Level {
        @Test
        void D_HIGH() {
            SubmissionDetail d = detail(ScoringMode.LEVEL, "D", null, null);
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.HIGH, v.getSeverity());
        }

        @Test
        void C_MEDIUM() {
            SubmissionDetail d = detail(ScoringMode.LEVEL, "C", null, null);
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.MEDIUM, v.getSeverity());
        }

        @Test
        void B_NONE() {
            SubmissionDetail d = detail(ScoringMode.LEVEL, "B", null, null);
            CorrectionVerdict v = engine.judge(d, normal, 0);
            // B sev=0.25 < l=0.3 → NONE
            assertEquals(Severity.NONE, v.getSeverity());
        }

        @Test
        void A_NONE() {
            SubmissionDetail d = detail(ScoringMode.LEVEL, "A", null, null);
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.NONE, v.getSeverity());
        }
    }

    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("RATING_SCALE")
    class Rating {
        @Test
        void one_star_HIGH() {
            SubmissionDetail d = detail(ScoringMode.RATING_SCALE, "1", null, null);
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.HIGH, v.getSeverity());
        }

        @Test
        void two_star_MEDIUM() {
            // sev = (5-2)/4 = 0.75 → MEDIUM (h=0.8)
            SubmissionDetail d = detail(ScoringMode.RATING_SCALE, "2", null, null);
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.MEDIUM, v.getSeverity());
        }

        @Test
        void five_star_NONE() {
            SubmissionDetail d = detail(ScoringMode.RATING_SCALE, "5", null, null);
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.NONE, v.getSeverity());
        }
    }

    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DIRECT")
    class Direct {
        @Test
        void zero_score_HIGH() {
            // 0/100 → sev=1.0 → HIGH
            SubmissionDetail d = detail(ScoringMode.DIRECT, null,
                    BigDecimal.ZERO, new BigDecimal("100"));
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.HIGH, v.getSeverity());
        }

        @Test
        void low_score_MEDIUM() {
            // 50/100 → sev=0.5 → MEDIUM
            SubmissionDetail d = detail(ScoringMode.DIRECT, null,
                    new BigDecimal("50"), new BigDecimal("100"));
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.MEDIUM, v.getSeverity());
        }

        @Test
        void high_score_NONE() {
            // 80/100 → sev=0.2 < l=0.3 → NONE
            SubmissionDetail d = detail(ScoringMode.DIRECT, null,
                    new BigDecimal("80"), new BigDecimal("100"));
            CorrectionVerdict v = engine.judge(d, normal, 0);
            assertEquals(Severity.NONE, v.getSeverity());
        }
    }

    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Strictness Presets")
    class Strictness {
        @Test
        void STRICT_lowers_thresholds() {
            // 30% 扣分 在 STRICT 模式下 → MEDIUM (m=0.3)
            ProjectCorrectivePolicy strict = new ProjectCorrectivePolicy(
                    "STRICT", SeverityThresholds.STRICT, DeadlinePresets.DEFAULT);
            SubmissionDetail d = detail(ScoringMode.DEDUCTION, null,
                    new BigDecimal("-3"), new BigDecimal("10"));
            CorrectionVerdict v = engine.judge(d, strict, 0);
            assertEquals(Severity.MEDIUM, v.getSeverity());
            assertTrue(v.isMustCorrect());
        }

        @Test
        void LENIENT_raises_thresholds() {
            // 50% 扣分 在 LENIENT (l=0.5) 下 → LOW (而非 NORMAL 的 MEDIUM)
            ProjectCorrectivePolicy lenient = new ProjectCorrectivePolicy(
                    "LENIENT", SeverityThresholds.LENIENT, DeadlinePresets.DEFAULT);
            SubmissionDetail d = detail(ScoringMode.DEDUCTION, null,
                    new BigDecimal("-5"), new BigDecimal("10"));
            CorrectionVerdict v = engine.judge(d, lenient, 0);
            assertEquals(Severity.LOW, v.getSeverity());
        }

        @Test
        void OFF_skips_engine() {
            ProjectCorrectivePolicy off = new ProjectCorrectivePolicy(
                    "OFF", SeverityThresholds.NORMAL, DeadlinePresets.DEFAULT);
            SubmissionDetail d = detail(ScoringMode.PASS_FAIL, "FAIL", null, null);
            CorrectionVerdict v = engine.judge(d, off, 0);
            assertEquals(Severity.NONE, v.getSeverity());
            assertFalse(v.shouldSuggest());
        }
    }

    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Recurrence")
    class Recurrence {
        @Test
        void first_recur_escalates_LOW_to_MEDIUM() {
            SubmissionDetail d = detail(ScoringMode.DEDUCTION, null,
                    new BigDecimal("-3"), new BigDecimal("10"));  // sev=0.3 → LOW
            CorrectionVerdict v = engine.judge(d, normal, 1);
            assertEquals(Severity.MEDIUM, v.getSeverity());
        }

        @Test
        void three_recurrence_forces_must_correct() {
            SubmissionDetail d = detail(ScoringMode.DEDUCTION, null,
                    new BigDecimal("-3"), new BigDecimal("10"));
            CorrectionVerdict v = engine.judge(d, normal, 3);
            assertTrue(v.isMustCorrect());
        }

        @Test
        void NONE_not_escalated_by_recurrence() {
            SubmissionDetail d = detail(ScoringMode.PASS_FAIL, "PASS", null, null);
            CorrectionVerdict v = engine.judge(d, normal, 5);
            assertEquals(Severity.NONE, v.getSeverity());
        }
    }

    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Trace 可解释性")
    class Trace {
        @Test
        void trace_records_each_layer() {
            SubmissionDetail d = detail(ScoringMode.DEDUCTION, null,
                    new BigDecimal("-7"), new BigDecimal("10"));
            CorrectionVerdict v = engine.judge(d, normal, 1);
            // normalize + threshold + recurrence
            assertTrue(v.getTrace().size() >= 3);
            assertEquals("normalize", v.getTrace().get(0).layer());
            assertEquals("threshold", v.getTrace().get(1).layer());
        }
    }
}
