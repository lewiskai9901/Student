package com.school.management.domain.inspection.model.v7.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InspTask 聚合根测试")
class InspTaskTest {

    // ==================== 工厂方法 ====================

    private InspTask createPendingTask() {
        return InspTask.create("TASK-001", 1L, LocalDate.of(2026, 3, 1));
    }

    private InspTask createClaimedTask() {
        InspTask task = createPendingTask();
        task.claim(10L, "检查员A");
        return task;
    }

    private InspTask createInProgressTask() {
        InspTask task = createClaimedTask();
        task.start();
        return task;
    }

    private InspTask createSubmittedTask() {
        InspTask task = createInProgressTask();
        task.submit();
        return task;
    }

    private InspTask createUnderReviewTask() {
        InspTask task = createSubmittedTask();
        task.startReview(20L, "审核员B");
        return task;
    }

    private InspTask createReviewedTask() {
        InspTask task = createUnderReviewTask();
        task.review("审核通过，无问题");
        return task;
    }

    // ==================== 创建测试 ====================

    @Nested
    @DisplayName("创建任务")
    class CreateTests {

        @Test
        @DisplayName("新建任务初始状态为PENDING")
        void testCreate() {
            InspTask task = InspTask.create("TASK-001", 1L, LocalDate.of(2026, 3, 1));

            assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
            assertThat(task.getTaskCode()).isEqualTo("TASK-001");
            assertThat(task.getProjectId()).isEqualTo(1L);
            assertThat(task.getTaskDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(task.getTotalTargets()).isEqualTo(0);
            assertThat(task.getCompletedTargets()).isEqualTo(0);
            assertThat(task.getSkippedTargets()).isEqualTo(0);
            assertThat(task.getCreatedAt()).isNotNull();
        }
    }

    // ==================== 领取测试 ====================

    @Nested
    @DisplayName("领取任务")
    class ClaimTests {

        @Test
        @DisplayName("PENDING → CLAIMED 正常领取")
        void testClaim() {
            InspTask task = createPendingTask();

            task.claim(10L, "检查员A");

            assertThat(task.getStatus()).isEqualTo(TaskStatus.CLAIMED);
            assertThat(task.getInspectorId()).isEqualTo(10L);
            assertThat(task.getInspectorName()).isEqualTo("检查员A");
        }

        @Test
        @DisplayName("非PENDING状态领取应抛异常")
        void testClaim_InvalidState() {
            InspTask task = createClaimedTask();

            assertThatThrownBy(() -> task.claim(20L, "检查员B"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有待领取的任务才能被领取");
        }
    }

    // ==================== 开始检查测试 ====================

    @Nested
    @DisplayName("开始检查")
    class StartTests {

        @Test
        @DisplayName("CLAIMED → IN_PROGRESS 正常开始")
        void testStart() {
            InspTask task = createClaimedTask();

            task.start();

            assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("非CLAIMED状态开始应抛异常")
        void testStart_InvalidState() {
            InspTask task = createPendingTask();

            assertThatThrownBy(() -> task.start())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已领取的任务才能开始检查");
        }
    }

    // ==================== 提交测试 ====================

    @Nested
    @DisplayName("提交任务")
    class SubmitTests {

        @Test
        @DisplayName("IN_PROGRESS → SUBMITTED 正常提交")
        void testSubmit() {
            InspTask task = createInProgressTask();

            task.submit();

            assertThat(task.getStatus()).isEqualTo(TaskStatus.SUBMITTED);
            assertThat(task.getSubmittedAt()).isNotNull();
        }

        @Test
        @DisplayName("非IN_PROGRESS状态提交应抛异常")
        void testSubmit_InvalidState() {
            InspTask task = createClaimedTask();

            assertThatThrownBy(() -> task.submit())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有进行中的任务才能提交");
        }
    }

    // ==================== 审核测试 ====================

    @Nested
    @DisplayName("审核")
    class ReviewTests {

        @Test
        @DisplayName("SUBMITTED → UNDER_REVIEW 开始审核")
        void testStartReview() {
            InspTask task = createSubmittedTask();

            task.startReview(20L, "审核员B");

            assertThat(task.getStatus()).isEqualTo(TaskStatus.UNDER_REVIEW);
            assertThat(task.getReviewerId()).isEqualTo(20L);
            assertThat(task.getReviewerName()).isEqualTo("审核员B");
        }

        @Test
        @DisplayName("UNDER_REVIEW → REVIEWED 完成审核")
        void testReview() {
            InspTask task = createUnderReviewTask();

            task.review("检查结果无异常");

            assertThat(task.getStatus()).isEqualTo(TaskStatus.REVIEWED);
            assertThat(task.getReviewComment()).isEqualTo("检查结果无异常");
            assertThat(task.getReviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("非SUBMITTED状态开始审核应抛异常")
        void testStartReview_InvalidState() {
            InspTask task = createInProgressTask();

            assertThatThrownBy(() -> task.startReview(20L, "审核员B"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已提交的任务才能审核");
        }

        @Test
        @DisplayName("非UNDER_REVIEW状态完成审核应抛异常")
        void testReview_InvalidState() {
            InspTask task = createSubmittedTask();

            assertThatThrownBy(() -> task.review("测试"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有审核中的任务才能完成审核");
        }
    }

    // ==================== 发布测试 ====================

    @Nested
    @DisplayName("发布结果")
    class PublishTests {

        @Test
        @DisplayName("REVIEWED → PUBLISHED 正常发布")
        void testPublish_FromReviewed() {
            InspTask task = createReviewedTask();

            task.publish();

            assertThat(task.getStatus()).isEqualTo(TaskStatus.PUBLISHED);
            assertThat(task.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("SUBMITTED → PUBLISHED 无需审核直接发布")
        void testPublish_FromSubmitted() {
            InspTask task = createSubmittedTask();

            task.publish();

            assertThat(task.getStatus()).isEqualTo(TaskStatus.PUBLISHED);
            assertThat(task.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("非REVIEWED/SUBMITTED状态发布应抛异常")
        void testPublish_InvalidState() {
            InspTask task = createInProgressTask();

            assertThatThrownBy(() -> task.publish())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已审核或已提交的任务才能发布");
        }
    }

    // ==================== 取消测试 ====================

    @Nested
    @DisplayName("取消任务")
    class CancelTests {

        @Test
        @DisplayName("PENDING → CANCELLED 正常取消")
        void testCancel_FromPending() {
            InspTask task = createPendingTask();

            task.cancel();

            assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        }

        @Test
        @DisplayName("CLAIMED → CANCELLED 领取后取消")
        void testCancel_FromClaimed() {
            InspTask task = createClaimedTask();

            task.cancel();

            assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        }

        @Test
        @DisplayName("IN_PROGRESS → CANCELLED 进行中取消")
        void testCancel_FromInProgress() {
            InspTask task = createInProgressTask();

            task.cancel();

            assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        }

        @Test
        @DisplayName("已发布任务不能取消")
        void testCancel_PublishedThrows() {
            InspTask task = createReviewedTask();
            task.publish();

            assertThatThrownBy(() -> task.cancel())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("已发布或已取消的任务不能取消");
        }

        @Test
        @DisplayName("已取消任务不能再次取消")
        void testCancel_CancelledThrows() {
            InspTask task = createPendingTask();
            task.cancel();

            assertThatThrownBy(() -> task.cancel())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("已发布或已取消的任务不能取消");
        }
    }

    // ==================== 过期测试 ====================

    @Nested
    @DisplayName("任务过期")
    class ExpireTests {

        @Test
        @DisplayName("PENDING → EXPIRED 正常过期")
        void testExpire_FromPending() {
            InspTask task = createPendingTask();

            task.expire();

            assertThat(task.getStatus()).isEqualTo(TaskStatus.EXPIRED);
        }

        @Test
        @DisplayName("CLAIMED → EXPIRED 领取后过期")
        void testExpire_FromClaimed() {
            InspTask task = createClaimedTask();

            task.expire();

            assertThat(task.getStatus()).isEqualTo(TaskStatus.EXPIRED);
        }

        @Test
        @DisplayName("IN_PROGRESS 状态不能过期")
        void testExpire_InProgressThrows() {
            InspTask task = createInProgressTask();

            assertThatThrownBy(() -> task.expire())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有待领取或已领取的任务才能过期");
        }
    }

    // ==================== 分配和目标计数测试 ====================

    @Nested
    @DisplayName("辅助操作")
    class AuxiliaryTests {

        @Test
        @DisplayName("分配检查员")
        void testAssign() {
            InspTask task = createPendingTask();

            task.assign(10L, "检查员A");

            assertThat(task.getInspectorId()).isEqualTo(10L);
            assertThat(task.getInspectorName()).isEqualTo("检查员A");
            assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING); // 仅分配，不改状态
        }

        @Test
        @DisplayName("非PENDING状态分配应抛异常")
        void testAssign_InvalidState() {
            InspTask task = createClaimedTask();

            assertThatThrownBy(() -> task.assign(20L, "检查员B"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有待领取的任务才能分配检查员");
        }

        @Test
        @DisplayName("更新目标计数")
        void testUpdateTargetCounts() {
            InspTask task = createPendingTask();

            task.updateTargetCounts(10, 5, 2);

            assertThat(task.getTotalTargets()).isEqualTo(10);
            assertThat(task.getCompletedTargets()).isEqualTo(5);
            assertThat(task.getSkippedTargets()).isEqualTo(2);
        }
    }

    // ==================== 完整生命周期测试 ====================

    @Nested
    @DisplayName("完整生命周期")
    class LifecycleTests {

        @Test
        @DisplayName("完整正常流程: PENDING → CLAIMED → IN_PROGRESS → SUBMITTED → UNDER_REVIEW → REVIEWED → PUBLISHED")
        void testFullLifecycle() {
            InspTask task = InspTask.create("TASK-LC", 1L, LocalDate.of(2026, 3, 1));
            assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);

            task.claim(10L, "检查员A");
            assertThat(task.getStatus()).isEqualTo(TaskStatus.CLAIMED);

            task.start();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

            task.submit();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.SUBMITTED);

            task.startReview(20L, "审核员B");
            assertThat(task.getStatus()).isEqualTo(TaskStatus.UNDER_REVIEW);

            task.review("一切正常");
            assertThat(task.getStatus()).isEqualTo(TaskStatus.REVIEWED);

            task.publish();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.PUBLISHED);

            assertThat(task.getSubmittedAt()).isNotNull();
            assertThat(task.getReviewedAt()).isNotNull();
            assertThat(task.getPublishedAt()).isNotNull();
        }
    }
}
