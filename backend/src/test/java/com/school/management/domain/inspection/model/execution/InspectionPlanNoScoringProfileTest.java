package com.school.management.domain.inspection.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防回退守护 — 评级引擎完美架构 (2026-05-23, Phase 6).
 *
 * <p>调度组 {@link InspectionPlan} 只负责 "什么时候/几人评/检查谁" — 调度维度,
 * 与评分算法 (ScoringProfile) / 评级 (Indicator) 完全解耦.
 *
 * <p>本测试守护 InspectionPlan 不再持有 scoringProfileId 或任何评分配置字段.
 */
@DisplayName("InspectionPlan 防回退 — 不得持有评分/评级配置字段")
class InspectionPlanNoScoringProfileTest {

    /** 禁止出现在 InspectionPlan 上的评分/评级字段名。 */
    private static final List<String> FORBIDDEN = Arrays.asList(
            "scoringProfileId", "defaultScoringProfileId",
            "gradeSchemeId", "indicatorId",
            "evaluationMode", "multiRaterMode", "raterWeightBy",
            "trendEnabled", "decayEnabled", "calibrationEnabled");

    @Test
    @DisplayName("InspectionPlan 不含 scoringProfileId 或评分配置字段")
    void inspectionPlanHasNoScoringField() {
        List<String> fields = Arrays.stream(InspectionPlan.class.getDeclaredFields())
                .map(Field::getName)
                .toList();
        for (String forbidden : FORBIDDEN) {
            assertThat(fields)
                    .as("评分/评级字段 '%s' 不应出现在 InspectionPlan — 调度组与评分评级解耦",
                            forbidden)
                    .doesNotContain(forbidden);
        }
    }
}
