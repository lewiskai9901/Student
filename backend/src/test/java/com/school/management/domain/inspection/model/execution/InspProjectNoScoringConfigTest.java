package com.school.management.domain.inspection.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防回退守护 — 评级引擎完美架构 (2026-05-23).
 *
 * <p>评级"怎么算"的唯一权威是 {@code ScoringProfile} (按 (project, section) 自动定位);
 * 调度组 {@code InspectionPlan} 只负责 raters; 项目本身完全不持有评分配置指针.
 *
 * <p>本测试守护 {@link InspProject} 不持有任何评分规则字段也不持有 ScoringProfile 指针.
 */
@DisplayName("InspProject 防回退 — 不得持有评分配置字段")
class InspProjectNoScoringConfigTest {

    /** 禁止出现在 InspProject 上的评分配置字段名。 */
    private static final List<String> FORBIDDEN = Arrays.asList(
            "evaluationMode", "multiRaterMode", "raterWeightBy", "consensusThreshold",
            "trendEnabled", "trendLookbackDays", "decayEnabled", "decayMode",
            "calibrationEnabled", "calibrationMethod", "splitStrategy",
            "scoringProfileId", "defaultScoringProfileId");

    @Test
    @DisplayName("InspProject 不含任何评分规则字段")
    void inspProjectHasNoScoringConfigField() {
        List<String> fields = Arrays.stream(InspProject.class.getDeclaredFields())
                .map(Field::getName)
                .toList();
        for (String forbidden : FORBIDDEN) {
            assertThat(fields)
                    .as("评分配置字段 '%s' 不应出现在 InspProject — 评级引擎完美架构后由 (project, section) 自动定位",
                            forbidden)
                    .doesNotContain(forbidden);
        }
    }
}
