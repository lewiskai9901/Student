package com.school.management.domain.inspection.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * InspectionPlan 调度组聚合根测试 — 评级引擎完美架构 (2026-05-23).
 *
 * <p>scoringProfileId 已撤销, 只剩 ratersPerTarget.
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
        @DisplayName("新建调度组 ratersPerTarget 默认 1")
        void shouldDefaultRatersToOne() {
            InspectionPlan plan = newPlan();
            assertThat(plan.getRatersPerTarget()).isEqualTo(1);
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
    @DisplayName("updateRatersPerTarget")
    class UpdateRatersTests {

        @Test
        @DisplayName("正常更新 ratersPerTarget")
        void shouldUpdateRaters() {
            InspectionPlan plan = newPlan();
            plan.updateRatersPerTarget(3);
            assertThat(plan.getRatersPerTarget()).isEqualTo(3);
            assertThat(plan.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("ratersPerTarget < 1 抛 IllegalArgumentException")
        void shouldRejectRatersBelowOne() {
            InspectionPlan plan = newPlan();
            assertThatThrownBy(() -> plan.updateRatersPerTarget(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ratersPerTarget");
            assertThatThrownBy(() -> plan.updateRatersPerTarget(-2))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("ratersPerTarget = 1 (单人评分) 合法")
        void shouldAcceptRatersEqualsOne() {
            InspectionPlan plan = newPlan();
            plan.updateRatersPerTarget(1);
            assertThat(plan.getRatersPerTarget()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("持久化往返 — builder 携带 ratersPerTarget")
    class ReconstructTests {

        @Test
        @DisplayName("reconstruct 还原 ratersPerTarget")
        void shouldReconstruct() {
            InspectionPlan plan = InspectionPlan.reconstruct(
                    InspectionPlan.builder().id(9L).projectId(1L).planName("P")
                            .ratersPerTarget(4));
            assertThat(plan.getRatersPerTarget()).isEqualTo(4);
        }
    }
}
