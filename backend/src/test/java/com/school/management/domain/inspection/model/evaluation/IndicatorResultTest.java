package com.school.management.domain.inspection.model.evaluation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IndicatorResult 聚合根测试 — 状态机 + 修订链.
 */
@DisplayName("IndicatorResult 聚合根")
class IndicatorResultTest {

    private IndicatorResult draft() {
        return IndicatorResult.draft(10L, 100L, "T", "2026-W22",
                new BigDecimal("85.0"), 1, "A", List.of(1L, 2L), List.of(50L));
    }

    @Test
    @DisplayName("draft 工厂返回 DRAFT 状态")
    void draftStartsAsDraft() {
        IndicatorResult r = draft();
        assertThat(r.getStatus()).isEqualTo(ResultStatus.DRAFT);
        assertThat(r.getComputedAt()).isNotNull();
        assertThat(r.getPublishedAt()).isNull();
        assertThat(r.getValue()).isEqualByComparingTo("85.0");
        assertThat(r.getSourceSubmissionIds()).containsExactly(1L, 2L);
        assertThat(r.getSourceSectionIds()).containsExactly(50L);
    }

    @Test
    @DisplayName("publish: DRAFT → PUBLISHED")
    void publishMovesToPublished() {
        IndicatorResult r = draft();
        r.publish();
        assertThat(r.getStatus()).isEqualTo(ResultStatus.PUBLISHED);
        assertThat(r.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("publish 非 DRAFT 抛 IllegalState")
    void cannotPublishTwice() {
        IndicatorResult r = draft();
        r.publish();
        assertThatThrownBy(r::publish).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("draft 工厂 indicatorId / targetId / periodKey 缺失抛")
    void draftRequiresFields() {
        assertThatThrownBy(() -> IndicatorResult.draft(null, 1L, "T", "P",
                BigDecimal.ZERO, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IndicatorResult.draft(10L, null, "T", "P",
                BigDecimal.ZERO, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IndicatorResult.draft(10L, 1L, "T", "",
                BigDecimal.ZERO, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("supersedeWith: 旧 PUBLISHED 变 SUPERSEDED, 返回新 DRAFT (revisionOf=旧 id)")
    void supersedeChain() {
        IndicatorResult published = draft();
        published.setId(7L);
        published.publish();
        assertThat(published.getStatus()).isEqualTo(ResultStatus.PUBLISHED);

        IndicatorResult revision = published.supersedeWith(
                new BigDecimal("90.0"), 1, "A+",
                List.of(1L, 2L, 3L), List.of(50L));

        assertThat(published.getStatus()).isEqualTo(ResultStatus.SUPERSEDED);
        assertThat(revision.getStatus()).isEqualTo(ResultStatus.DRAFT);
        assertThat(revision.getRevisionOf()).isEqualTo(7L);
        assertThat(revision.getValue()).isEqualByComparingTo("90.0");
        assertThat(revision.getPeriodKey()).isEqualTo(published.getPeriodKey());
        assertThat(revision.getIndicatorId()).isEqualTo(published.getIndicatorId());
        assertThat(revision.getTargetId()).isEqualTo(published.getTargetId());
    }

    @Test
    @DisplayName("supersedeWith 非 PUBLISHED 抛 IllegalState")
    void cannotSupersedeDraft() {
        IndicatorResult r = draft();
        assertThatThrownBy(() -> r.supersedeWith(BigDecimal.ZERO, null, null, null, null))
                .isInstanceOf(IllegalStateException.class);
    }
}
