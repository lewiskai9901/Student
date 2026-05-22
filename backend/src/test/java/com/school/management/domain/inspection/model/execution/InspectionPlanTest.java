package com.school.management.domain.inspection.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * InspectionPlan 调度组聚合根测试 — 评分配置下沉 (2026-05-23).
 */
@DisplayName("InspectionPlan 调度组聚合根测试")
class InspectionPlanTest {

    private InspectionPlan newPlan() {
        return InspectionPlan.create(1L, "调度组A", null, 100L);
    }

    @Nested
    @DisplayName("create — 工厂方法默认值")
    class CreateTests {

        @Test
        @DisplayName("新建调度组 ratersPerTarget 默认 1, scoringProfileId 默认 null")
        void shouldDefaultRatersToOne() {
            InspectionPlan plan = newPlan();
            assertThat(plan.getRatersPerTarget()).isEqualTo(1);
            assertThat(plan.getScoringProfileId()).isNull();
            assertThat(plan.getIsEnabled()).isTrue();
        }

        @Test
        @DisplayName("getRatersPerTarget 对 null 字段兜底返回 1")
        void shouldFallbackRatersToOneWhenNull() {
            InspectionPlan plan = InspectionPlan.reconstruct(
                    InspectionPlan.builder().id(5L).projectId(1L).planName("P")
                            .ratersPerTarget(null));
            assertThat(plan.getRatersPerTarget()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("updateScoringConfig — 评分配置下沉")
    class UpdateScoringConfigTests {

        @Test
        @DisplayName("正常更新 scoringProfileId + ratersPerTarget")
        void shouldUpdateScoringConfig() {
            InspectionPlan plan = newPlan();
            plan.updateScoringConfig(777L, 3);
            assertThat(plan.getScoringProfileId()).isEqualTo(777L);
            assertThat(plan.getRatersPerTarget()).isEqualTo(3);
            assertThat(plan.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("scoringProfileId 允许置空 (回退项目默认评分方案)")
        void shouldAllowNullScoringProfileId() {
            InspectionPlan plan = newPlan();
            plan.updateScoringConfig(777L, 1);
            plan.updateScoringConfig(null, 1);
            assertThat(plan.getScoringProfileId()).isNull();
        }

        @Test
        @DisplayName("ratersPerTarget < 1 抛 IllegalArgumentException")
        void shouldRejectRatersBelowOne() {
            InspectionPlan plan = newPlan();
            assertThatThrownBy(() -> plan.updateScoringConfig(1L, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ratersPerTarget");
            assertThatThrownBy(() -> plan.updateScoringConfig(1L, -2))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("ratersPerTarget = 1 (单人评分) 合法")
        void shouldAcceptRatersEqualsOne() {
            InspectionPlan plan = newPlan();
            plan.updateScoringConfig(1L, 1);
            assertThat(plan.getRatersPerTarget()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("持久化往返 — builder 携带评分字段")
    class ReconstructTests {

        @Test
        @DisplayName("reconstruct 还原 scoringProfileId / ratersPerTarget")
        void shouldReconstructScoringFields() {
            InspectionPlan plan = InspectionPlan.reconstruct(
                    InspectionPlan.builder().id(9L).projectId(1L).planName("P")
                            .scoringProfileId(555L).ratersPerTarget(4));
            assertThat(plan.getScoringProfileId()).isEqualTo(555L);
            assertThat(plan.getRatersPerTarget()).isEqualTo(4);
        }
    }
}
