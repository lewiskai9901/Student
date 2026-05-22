package com.school.management.domain.inspection.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防回退守护 — 评分配置下沉重构 (2026-05-23).
 *
 * <p>评分"怎么算"的唯一权威是 {@code ScoringProfile};调度组 {@code InspectionPlan}
 * 通过 {@code scoringProfileId} 引用规则并自带 {@code ratersPerTarget};项目仅保留
 * {@code defaultScoringProfileId} 作为非计划任务 (临时抽查/自查/触发) 的兜底。
 *
 * <p>本测试守护 {@link InspProject} 不再持有任何评分规则字段 —— 防止后续有人
 * 把 evaluationMode / trend / decay / calibration 等重新加回项目聚合根。
 */
@DisplayName("InspProject 防回退 — 不得持有评分配置字段")
class InspProjectNoScoringConfigTest {

    /** 禁止出现在 InspProject 上的评分配置字段名 (scoringProfileId 已改名 defaultScoringProfileId)。 */
    private static final List<String> FORBIDDEN = Arrays.asList(
            "evaluationMode", "multiRaterMode", "raterWeightBy", "consensusThreshold",
            "trendEnabled", "trendLookbackDays", "decayEnabled", "decayMode",
            "calibrationEnabled", "calibrationMethod", "splitStrategy", "scoringProfileId");

    @Test
    @DisplayName("InspProject 不含任何评分规则字段")
    void inspProjectHasNoScoringConfigField() {
        List<String> fields = Arrays.stream(InspProject.class.getDeclaredFields())
                .map(Field::getName)
                .toList();
        for (String forbidden : FORBIDDEN) {
            assertThat(fields)
                    .as("评分配置字段 '%s' 不应出现在 InspProject — 评分规则只属于 ScoringProfile", forbidden)
                    .doesNotContain(forbidden);
        }
    }

    @Test
    @DisplayName("InspProject 保留 defaultScoringProfileId 作非计划任务兜底")
    void inspProjectKeepsDefaultScoringProfileId() {
        List<String> fields = Arrays.stream(InspProject.class.getDeclaredFields())
                .map(Field::getName)
                .toList();
        assertThat(fields).contains("defaultScoringProfileId");
    }
}
