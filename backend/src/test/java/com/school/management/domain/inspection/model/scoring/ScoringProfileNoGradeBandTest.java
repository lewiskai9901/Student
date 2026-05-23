package com.school.management.domain.inspection.model.scoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防回退守护 — 评级引擎完美架构 (2026-05-23, Phase 6).
 *
 * <p>{@link ScoringProfile} 是"算分"权威, "评级"权威是 {@link Indicator}.
 * ScoringProfile 不得再持有 GradeBand 关联 — GradeBand 表保留为历史遗留,
 * 评级阈值由 Indicator.gradeSchemeId 关联到 GradeScheme.
 *
 * <p>本测试守护 ScoringProfile 字段不含任何 gradeBand / grade 类引用.
 */
@DisplayName("ScoringProfile 防回退 — 不得持有评级字段")
class ScoringProfileNoGradeBandTest {

    /** 禁止出现在 ScoringProfile 上的评级字段。 */
    private static final List<String> FORBIDDEN = Arrays.asList(
            "gradeBands", "gradeBandIds",
            "gradeScheme", "gradeSchemeId",
            "indicators", "indicatorIds");

    @Test
    @DisplayName("ScoringProfile 不含 GradeBand / GradeScheme / Indicator 关联")
    void scoringProfileHasNoGradingField() {
        List<String> fields = Arrays.stream(ScoringProfile.class.getDeclaredFields())
                .map(Field::getName)
                .toList();
        for (String forbidden : FORBIDDEN) {
            assertThat(fields)
                    .as("评级字段 '%s' 不应出现在 ScoringProfile — 算分 vs 评级正交解耦",
                            forbidden)
                    .doesNotContain(forbidden);
        }
    }
}
