package com.school.management.domain.inspection.model.v7.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InspSubmission 聚合根测试")
class InspSubmissionTest {

    // ==================== 工厂方法 ====================

    private InspSubmission createPendingSubmission() {
        return InspSubmission.create(1L, TargetType.ORG, 100L, "一年级一班");
    }

    private InspSubmission createLockedSubmission() {
        InspSubmission sub = createPendingSubmission();
        sub.lock();
        return sub;
    }

    private InspSubmission createInProgressSubmission() {
        InspSubmission sub = createPendingSubmission();
        sub.startFilling();
        return sub;
    }

    // ==================== 创建测试 ====================

    @Nested
    @DisplayName("创建提交")
    class CreateTests {

        @Test
        @DisplayName("新建提交初始状态为PENDING")
        void testCreate() {
            InspSubmission sub = InspSubmission.create(
                    1L, TargetType.ORG, 100L, "一年级一班");

            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.PENDING);
            assertThat(sub.getTaskId()).isEqualTo(1L);
            assertThat(sub.getTargetType()).isEqualTo(TargetType.ORG);
            assertThat(sub.getTargetId()).isEqualTo(100L);
            assertThat(sub.getTargetName()).isEqualTo("一年级一班");
            assertThat(sub.getWeightRatio()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(sub.getDeductionTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(sub.getBonusTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(sub.getSyncVersion()).isEqualTo(1);
            assertThat(sub.getCreatedAt()).isNotNull();
        }
    }

    // ==================== 锁定测试 ====================

    @Nested
    @DisplayName("锁定提交")
    class LockTests {

        @Test
        @DisplayName("PENDING → LOCKED 正常锁定")
        void testLock() {
            InspSubmission sub = createPendingSubmission();

            sub.lock();

            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.LOCKED);
        }

        @Test
        @DisplayName("非PENDING状态锁定应抛异常")
        void testLock_InvalidState() {
            InspSubmission sub = createLockedSubmission();

            assertThatThrownBy(() -> sub.lock())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有待检查的提交才能锁定");
        }
    }

    // ==================== 解锁测试 ====================

    @Nested
    @DisplayName("解锁提交")
    class UnlockTests {

        @Test
        @DisplayName("LOCKED → PENDING 正常解锁")
        void testUnlock() {
            InspSubmission sub = createLockedSubmission();

            sub.unlock();

            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        }

        @Test
        @DisplayName("非LOCKED状态解锁应抛异常")
        void testUnlock_InvalidState() {
            InspSubmission sub = createPendingSubmission();

            assertThatThrownBy(() -> sub.unlock())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已锁定的提交才能解锁");
        }
    }

    // ==================== 开始填写测试 ====================

    @Nested
    @DisplayName("开始填写")
    class StartFillingTests {

        @Test
        @DisplayName("PENDING → IN_PROGRESS 正常开始填写")
        void testStartFilling_FromPending() {
            InspSubmission sub = createPendingSubmission();

            sub.startFilling();

            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("LOCKED → IN_PROGRESS 从锁定状态开始填写")
        void testStartFilling_FromLocked() {
            InspSubmission sub = createLockedSubmission();

            sub.startFilling();

            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("COMPLETED状态不能开始填写")
        void testStartFilling_InvalidState() {
            InspSubmission sub = createInProgressSubmission();
            sub.complete(new BigDecimal("100"), new BigDecimal("95"),
                    new BigDecimal("-5"), BigDecimal.ZERO, "{}", "A", true);

            assertThatThrownBy(() -> sub.startFilling())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有待检查或已锁定的提交才能开始填写");
        }
    }

    // ==================== 保存表单数据测试 ====================

    @Nested
    @DisplayName("保存表单数据")
    class SaveFormDataTests {

        @Test
        @DisplayName("IN_PROGRESS 状态保存表单数据")
        void testSaveFormData_InProgress() {
            InspSubmission sub = createInProgressSubmission();
            int originalVersion = sub.getSyncVersion();

            sub.saveFormData("{\"item1\": \"value1\"}");

            assertThat(sub.getFormData()).isEqualTo("{\"item1\": \"value1\"}");
            assertThat(sub.getSyncVersion()).isEqualTo(originalVersion + 1);
        }

        @Test
        @DisplayName("LOCKED 状态保存表单数据")
        void testSaveFormData_Locked() {
            InspSubmission sub = createLockedSubmission();

            sub.saveFormData("{\"draft\": true}");

            assertThat(sub.getFormData()).isEqualTo("{\"draft\": true}");
        }

        @Test
        @DisplayName("多次保存递增 syncVersion")
        void testSaveFormData_IncrementsSyncVersion() {
            InspSubmission sub = createInProgressSubmission();
            int v1 = sub.getSyncVersion();

            sub.saveFormData("{\"v1\": true}");
            assertThat(sub.getSyncVersion()).isEqualTo(v1 + 1);

            sub.saveFormData("{\"v2\": true}");
            assertThat(sub.getSyncVersion()).isEqualTo(v1 + 2);
        }

        @Test
        @DisplayName("PENDING状态不能保存表单数据")
        void testSaveFormData_InvalidState() {
            InspSubmission sub = createPendingSubmission();

            assertThatThrownBy(() -> sub.saveFormData("{\"test\": true}"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有进行中或已锁定的提交才能保存");
        }
    }

    // ==================== 完成提交测试 ====================

    @Nested
    @DisplayName("完成提交")
    class CompleteTests {

        @Test
        @DisplayName("IN_PROGRESS → COMPLETED 正常完成")
        void testComplete() {
            InspSubmission sub = createInProgressSubmission();

            sub.complete(
                    new BigDecimal("100"),
                    new BigDecimal("92.50"),
                    new BigDecimal("-7.50"),
                    BigDecimal.ZERO,
                    "{\"details\": []}",
                    "A",
                    true
            );

            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
            assertThat(sub.getBaseScore()).isEqualByComparingTo(new BigDecimal("100"));
            assertThat(sub.getFinalScore()).isEqualByComparingTo(new BigDecimal("92.50"));
            assertThat(sub.getDeductionTotal()).isEqualByComparingTo(new BigDecimal("-7.50"));
            assertThat(sub.getBonusTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(sub.getScoreBreakdown()).isEqualTo("{\"details\": []}");
            assertThat(sub.getGrade()).isEqualTo("A");
            assertThat(sub.getPassed()).isTrue();
            assertThat(sub.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("非IN_PROGRESS状态完成应抛异常")
        void testComplete_InvalidState() {
            InspSubmission sub = createPendingSubmission();

            assertThatThrownBy(() -> sub.complete(
                    new BigDecimal("100"), new BigDecimal("90"),
                    new BigDecimal("-10"), BigDecimal.ZERO, "{}", "A", true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有进行中的提交才能完成");
        }

        @Test
        @DisplayName("LOCKED状态完成应抛异常（需要先startFilling）")
        void testComplete_FromLockedThrows() {
            InspSubmission sub = createLockedSubmission();

            assertThatThrownBy(() -> sub.complete(
                    new BigDecimal("100"), new BigDecimal("90"),
                    new BigDecimal("-10"), BigDecimal.ZERO, "{}", "A", true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有进行中的提交才能完成");
        }
    }

    // ==================== 跳过测试 ====================

    @Nested
    @DisplayName("跳过提交")
    class SkipTests {

        @Test
        @DisplayName("PENDING → SKIPPED 正常跳过")
        void testSkip_FromPending() {
            InspSubmission sub = createPendingSubmission();

            sub.skip();

            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.SKIPPED);
        }

        @Test
        @DisplayName("LOCKED → SKIPPED 从锁定状态跳过")
        void testSkip_FromLocked() {
            InspSubmission sub = createLockedSubmission();

            sub.skip();

            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.SKIPPED);
        }

        @Test
        @DisplayName("IN_PROGRESS 状态不能跳过")
        void testSkip_InProgressThrows() {
            InspSubmission sub = createInProgressSubmission();

            assertThatThrownBy(() -> sub.skip())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有待检查或已锁定的提交才能跳过");
        }

        @Test
        @DisplayName("COMPLETED 状态不能跳过")
        void testSkip_CompletedThrows() {
            InspSubmission sub = createInProgressSubmission();
            sub.complete(new BigDecimal("100"), new BigDecimal("90"),
                    new BigDecimal("-10"), BigDecimal.ZERO, "{}", "A", true);

            assertThatThrownBy(() -> sub.skip())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有待检查或已锁定的提交才能跳过");
        }
    }

    // ==================== 完整生命周期测试 ====================

    @Nested
    @DisplayName("完整生命周期")
    class LifecycleTests {

        @Test
        @DisplayName("正常流程: PENDING → LOCKED → IN_PROGRESS → COMPLETED")
        void testFullLifecycle() {
            InspSubmission sub = InspSubmission.create(
                    1L, TargetType.PLACE, 200L, "301宿舍");

            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.PENDING);

            sub.lock();
            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.LOCKED);

            sub.startFilling();
            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.IN_PROGRESS);

            sub.saveFormData("{\"item1\": \"checked\"}");
            assertThat(sub.getSyncVersion()).isEqualTo(2); // 初始1 + 1次save

            sub.complete(
                    new BigDecimal("100"), new BigDecimal("88.00"),
                    new BigDecimal("-12.00"), BigDecimal.ZERO,
                    "{}", "B", true
            );

            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
            assertThat(sub.getFinalScore()).isEqualByComparingTo(new BigDecimal("88.00"));
            assertThat(sub.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("锁定-解锁-再锁定流程")
        void testLockUnlockCycle() {
            InspSubmission sub = createPendingSubmission();

            sub.lock();
            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.LOCKED);

            sub.unlock();
            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.PENDING);

            sub.lock();
            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.LOCKED);
        }

        @Test
        @DisplayName("跳过流程: PENDING → SKIPPED")
        void testSkipLifecycle() {
            InspSubmission sub = createPendingSubmission();

            sub.skip();

            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.SKIPPED);
            assertThat(sub.getFinalScore()).isNull();
            assertThat(sub.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("使用reconstruct重建提交")
        void testReconstruct() {
            InspSubmission sub = InspSubmission.reconstruct(
                    InspSubmission.builder()
                            .id(999L)
                            .taskId(1L)
                            .targetType(TargetType.USER)
                            .targetId(50L)
                            .targetName("学生X")
                            .status(SubmissionStatus.COMPLETED)
                            .finalScore(new BigDecimal("95.50"))
                            .grade("A")
                            .passed(true)
                            .syncVersion(5)
            );

            assertThat(sub.getId()).isEqualTo(999L);
            assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
            assertThat(sub.getFinalScore()).isEqualByComparingTo(new BigDecimal("95.50"));
            assertThat(sub.getSyncVersion()).isEqualTo(5);
        }
    }
}
