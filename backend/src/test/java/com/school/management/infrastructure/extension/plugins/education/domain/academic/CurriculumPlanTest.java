package com.school.management.infrastructure.extension.plugins.education.domain.academic;

import com.school.management.infrastructure.extension.plugins.education.domain.academic.model.CurriculumPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CurriculumPlan 聚合根单元测试 — 验证创建、更新、发布、归档、复制等业务规则
 */
@DisplayName("CurriculumPlan 聚合根测试")
class CurriculumPlanTest {

    private static final Long CREATOR_ID = 1L;

    private CurriculumPlan plan;

    @BeforeEach
    void setUp() {
        plan = CurriculumPlan.create("PLAN001", "软件工程培养方案", CREATOR_ID);
    }

    @Nested
    @DisplayName("创建培养方案")
    class CreatePlanTests {

        @Test
        @DisplayName("应成功创建并使用默认值")
        void shouldCreateWithDefaults() {
            assertThat(plan.getPlanCode()).isEqualTo("PLAN001");
            assertThat(plan.getPlanName()).isEqualTo("软件工程培养方案");
            assertThat(plan.getCreatedBy()).isEqualTo(CREATOR_ID);
            assertThat(plan.getPlanVersion()).isEqualTo(1);
            assertThat(plan.getStatus()).isEqualTo(0); // 草稿
            assertThat(plan.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("planCode 为 null 抛 NPE")
        void shouldFailWhenCodeNull() {
            assertThatThrownBy(() -> CurriculumPlan.create(null, "n", CREATOR_ID))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("planName 为 null 抛 NPE")
        void shouldFailWhenNameNull() {
            assertThatThrownBy(() -> CurriculumPlan.create("PLAN001", null, CREATOR_ID))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("planCode 为空字符串抛 IllegalArgumentException")
        void shouldFailWhenCodeBlank() {
            assertThatThrownBy(() -> CurriculumPlan.create("  ", "n", CREATOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plan code");
        }

        @Test
        @DisplayName("planName 过长抛异常")
        void shouldFailWhenNameTooLong() {
            String longName = "n".repeat(101);
            assertThatThrownBy(() -> CurriculumPlan.create("PLAN001", longName, CREATOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceed 100");
        }
    }

    @Nested
    @DisplayName("更新方案信息")
    class UpdatePlanTests {

        @Test
        @DisplayName("应能更新方案字段")
        void shouldUpdate() {
            plan.update("新名称", 10L, 20L, 2024,
                    new BigDecimal("160"), new BigDecimal("120"),
                    new BigDecimal("30"), new BigDecimal("10"),
                    "培养目标 X", "毕业要求 Y", 99L);

            assertThat(plan.getPlanName()).isEqualTo("新名称");
            assertThat(plan.getMajorId()).isEqualTo(10L);
            assertThat(plan.getMajorDirectionId()).isEqualTo(20L);
            assertThat(plan.getGradeYear()).isEqualTo(2024);
            assertThat(plan.getTotalCredits()).isEqualByComparingTo("160");
            assertThat(plan.getRequiredCredits()).isEqualByComparingTo("120");
            assertThat(plan.getTrainingObjective()).isEqualTo("培养目标 X");
            assertThat(plan.getGraduationRequirement()).isEqualTo("毕业要求 Y");
            assertThat(plan.getUpdatedBy()).isEqualTo(99L);
        }

        @Test
        @DisplayName("planName 为 null 时保留原值")
        void shouldKeepNameWhenNull() {
            String old = plan.getPlanName();
            plan.update(null, null, null, null, null, null, null, null, null, null, 99L);
            assertThat(plan.getPlanName()).isEqualTo(old);
        }
    }

    @Nested
    @DisplayName("发布与归档")
    class PublishDeprecateTests {

        @Test
        @DisplayName("发布方案 — 状态变 1, publishedAt 记录")
        void shouldPublish() {
            plan.publish(99L);

            assertThat(plan.getStatus()).isEqualTo(1);
            assertThat(plan.getPublishedAt()).isNotNull();
            assertThat(plan.getPublishedBy()).isEqualTo(99L);
        }

        @Test
        @DisplayName("归档方案 — 状态变 2")
        void shouldDeprecate() {
            plan.deprecate();
            assertThat(plan.getStatus()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("复制创建新版本")
    class CopyWithNewVersionTests {

        @Test
        @DisplayName("复制时新方案版本号更新, 状态重置为草稿")
        void shouldCopyAndResetStatus() {
            plan.update("方案 V1", 10L, 20L, 2024,
                    new BigDecimal("160"), null, null, null,
                    "目标", "要求", 1L);
            plan.publish(99L); // 当前发布

            CurriculumPlan copy = plan.copyWithNewVersion(2, 88L);

            assertThat(copy.getPlanCode()).isEqualTo(plan.getPlanCode());
            assertThat(copy.getPlanName()).isEqualTo("方案 V1");
            assertThat(copy.getMajorId()).isEqualTo(10L);
            assertThat(copy.getPlanVersion()).isEqualTo(2);
            assertThat(copy.getStatus()).isEqualTo(0); // 回到草稿
            assertThat(copy.getCreatedBy()).isEqualTo(88L);
            assertThat(copy.getId()).isNull(); // 副本没 ID
        }

        @Test
        @DisplayName("复制后修改副本不影响原方案")
        void shouldCopyIsIndependent() {
            CurriculumPlan copy = plan.copyWithNewVersion(2, 88L);
            copy.update("修改后副本", null, null, null, null, null, null, null, null, null, 1L);

            assertThat(plan.getPlanName()).isEqualTo("软件工程培养方案");
            assertThat(copy.getPlanName()).isEqualTo("修改后副本");
        }
    }
}
