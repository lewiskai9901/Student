package com.school.management.domain.inspection.model.scoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Indicator 评级配置 (评级引擎完美架构, 2026-05-23) 测试.
 *
 * <p>覆盖: sourceSectionIds 校验 / triggerMode 各分支 / weightsBySection 子集校验 / enums 行为.
 */
@DisplayName("Indicator 评级引擎完美架构")
class IndicatorEvaluationConfigTest {

    private Indicator newLeaf() {
        return Indicator.createLeaf(1L, "leaf", 100L, "AVG", "PER_TASK", null);
    }

    @Nested
    @DisplayName("sourceSectionIds 校验")
    class SourceSectionIdsTests {

        @Test
        @DisplayName("空列表 → IllegalArgumentException")
        void shouldRejectEmptyList() {
            Indicator ind = newLeaf();
            assertThatThrownBy(() -> ind.updateEvaluationConfig(
                    List.of(), TriggerMode.TIME_WINDOW, null, null,
                    null, MissingPolicy.IGNORE, LatePolicy.REVISE_ORIGINAL,
                    SubmissionDateField.taskDate, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sourceSectionIds 至少 1 个");
        }

        @Test
        @DisplayName("含 null 元素 → IllegalArgumentException")
        void shouldRejectNullElement() {
            Indicator ind = newLeaf();
            java.util.List<Long> list = new java.util.ArrayList<>();
            list.add(1L); list.add(null);
            assertThatThrownBy(() -> ind.updateEvaluationConfig(
                    list, TriggerMode.TIME_WINDOW, null, null,
                    null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("单分区合法")
        void shouldAcceptSingleSection() {
            Indicator ind = newLeaf();
            ind.updateEvaluationConfig(List.of(100L), TriggerMode.TIME_WINDOW, null, null,
                    null, MissingPolicy.IGNORE, LatePolicy.REVISE_ORIGINAL,
                    SubmissionDateField.taskDate, null, "PER_TASK");
            assertThat(ind.getSourceSectionIds()).containsExactly(100L);
        }

        @Test
        @DisplayName("多分区合法")
        void shouldAcceptMultipleSections() {
            Indicator ind = newLeaf();
            ind.updateEvaluationConfig(List.of(100L, 200L, 300L),
                    TriggerMode.TIME_WINDOW, null, null,
                    null, null, null, null, null, null);
            assertThat(ind.getSourceSectionIds()).containsExactly(100L, 200L, 300L);
        }
    }

    @Nested
    @DisplayName("triggerMode 各分支")
    class TriggerModeTests {

        @Test
        @DisplayName("COUNT + countThreshold 未填 → 拒绝")
        void shouldRejectCountWithoutThreshold() {
            Indicator ind = newLeaf();
            assertThatThrownBy(() -> ind.updateEvaluationConfig(
                    List.of(100L), TriggerMode.COUNT, null, null,
                    null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("COUNT");
        }

        @Test
        @DisplayName("COUNT + countThreshold=0 → 拒绝")
        void shouldRejectCountThresholdZero() {
            Indicator ind = newLeaf();
            assertThatThrownBy(() -> ind.updateEvaluationConfig(
                    List.of(100L), TriggerMode.COUNT, 0, null,
                    null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("COUNT + countThreshold>=1 合法")
        void shouldAcceptCountWithThreshold() {
            Indicator ind = newLeaf();
            assertThatCode(() -> ind.updateEvaluationConfig(
                    List.of(100L), TriggerMode.COUNT, 5, null,
                    null, null, null, null, null, null))
                    .doesNotThrowAnyException();
            assertThat(ind.getTriggerMode()).isEqualTo(TriggerMode.COUNT);
            assertThat(ind.getCountThreshold()).isEqualTo(5);
        }

        @Test
        @DisplayName("TIME_WINDOW / MANUAL 不要求 countThreshold")
        void shouldAcceptTimeWindowAndManualWithoutThreshold() {
            Indicator ind = newLeaf();
            ind.updateEvaluationConfig(List.of(100L), TriggerMode.TIME_WINDOW,
                    null, null, null, null, null, null, null, null);
            assertThat(ind.getTriggerMode()).isEqualTo(TriggerMode.TIME_WINDOW);

            ind.updateEvaluationConfig(List.of(100L), TriggerMode.MANUAL,
                    null, null, null, null, null, null, null, null);
            assertThat(ind.getTriggerMode()).isEqualTo(TriggerMode.MANUAL);
        }

        @Test
        @DisplayName("triggerMode 默认 TIME_WINDOW")
        void shouldDefaultToTimeWindow() {
            Indicator ind = Indicator.createLeaf(1L, "n", 1L, "AVG", "PER_TASK", null);
            assertThat(ind.getTriggerMode()).isEqualTo(TriggerMode.TIME_WINDOW);
        }
    }

    @Nested
    @DisplayName("weightsBySection 子集校验")
    class WeightsTests {

        @Test
        @DisplayName("weights key 不在 sourceSectionIds 中 → 拒绝")
        void shouldRejectExtraneousWeightKey() {
            Indicator ind = newLeaf();
            Map<Long, BigDecimal> weights = new LinkedHashMap<>();
            weights.put(999L, new BigDecimal("0.5")); // 不在 [100L] 中
            assertThatThrownBy(() -> ind.updateEvaluationConfig(
                    List.of(100L), TriggerMode.TIME_WINDOW, null, weights,
                    null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("weightsBySection");
        }

        @Test
        @DisplayName("weights key 是 sourceSectionIds 子集 → 合法")
        void shouldAcceptSubsetWeights() {
            Indicator ind = newLeaf();
            Map<Long, BigDecimal> weights = new LinkedHashMap<>();
            weights.put(100L, new BigDecimal("0.4"));
            weights.put(200L, new BigDecimal("0.6"));
            ind.updateEvaluationConfig(List.of(100L, 200L, 300L),
                    TriggerMode.TIME_WINDOW, null, weights,
                    null, null, null, null, null, null);
            assertThat(ind.getWeightsBySection()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("enums 行为")
    class EnumTests {

        @Test
        @DisplayName("MissingPolicy.fromString SKIP / 空 → IGNORE")
        void missingPolicyAliasing() {
            assertThat(MissingPolicy.fromString("SKIP")).isEqualTo(MissingPolicy.IGNORE);
            assertThat(MissingPolicy.fromString("")).isEqualTo(MissingPolicy.IGNORE);
            assertThat(MissingPolicy.fromString(null)).isEqualTo(MissingPolicy.IGNORE);
            assertThat(MissingPolicy.fromString("ZERO")).isEqualTo(MissingPolicy.ZERO);
            assertThat(MissingPolicy.fromString("MAX")).isEqualTo(MissingPolicy.MAX);
            assertThat(MissingPolicy.fromString("WAIT")).isEqualTo(MissingPolicy.WAIT);
            assertThat(MissingPolicy.fromString("UNKNOWN")).isEqualTo(MissingPolicy.IGNORE);
        }

        @Test
        @DisplayName("RankDirection 可空 (null=不排名)")
        void rankDirectionNullable() {
            Indicator ind = newLeaf();
            ind.updateEvaluationConfig(List.of(100L), TriggerMode.TIME_WINDOW,
                    null, null, null, null, null, null, null, null);
            assertThat(ind.getRankDirection()).isNull();
            ind.updateEvaluationConfig(List.of(100L), TriggerMode.TIME_WINDOW,
                    null, null, RankDirection.ASC, null, null, null, null, null);
            assertThat(ind.getRankDirection()).isEqualTo(RankDirection.ASC);
        }

        @Test
        @DisplayName("LatePolicy 默认 REVISE_ORIGINAL")
        void latePolicyDefault() {
            Indicator ind = newLeaf();
            assertThat(ind.getLatePolicy()).isEqualTo(LatePolicy.REVISE_ORIGINAL);
        }

        @Test
        @DisplayName("SubmissionDateField 默认 taskDate")
        void submissionDateFieldDefault() {
            Indicator ind = newLeaf();
            assertThat(ind.getSubmissionDateField()).isEqualTo(SubmissionDateField.taskDate);
        }
    }
}
