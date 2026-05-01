package com.school.management.domain.inspection.model.execution;

import com.school.management.domain.inspection.event.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * InspTask 聚合根状态机单测.
 *
 * 状态机:
 *   PENDING → CLAIMED → IN_PROGRESS → SUBMITTED → UNDER_REVIEW → REVIEWED → PUBLISHED
 *
 * 横切动作:
 *   - reject (SUBMITTED/UNDER_REVIEW → IN_PROGRESS, 计 rejectionCount, 自动延期)
 *   - withdraw (SUBMITTED → IN_PROGRESS)
 *   - unclaim (CLAIMED/IN_PROGRESS → PENDING, review #D 离职重派)
 *   - cancel / expire / assign / extendDeadline / autoPublish 跳过审核
 */
@DisplayName("InspTask 聚合根状态机")
class InspTaskStateMachineTest {

    private static InspTask newPending() {
        InspTask t = InspTask.create("TSK-T-001", 100L, LocalDate.of(2026, 5, 1));
        t.clearDomainEvents();
        return t;
    }

    private static InspTask inState(TaskStatus status) {
        InspTask.Builder b = InspTask.builder()
                .id(1L).taskCode("TSK-T-001").projectId(100L)
                .taskDate(LocalDate.of(2026, 5, 1)).status(status);
        if (status != TaskStatus.PENDING && status != TaskStatus.EXPIRED) {
            b.inspectorId(10L).inspectorName("inspector");
        }
        if (status == TaskStatus.UNDER_REVIEW || status == TaskStatus.REVIEWED) {
            b.reviewerId(20L).reviewerName("reviewer");
        }
        return b.build();
    }

    // ============================================================
    @Nested
    @DisplayName("create — 工厂方法")
    class CreateTests {
        @Test
        @DisplayName("创建后 PENDING 且无 inspector, 注册 TaskCreatedEvent")
        void shouldCreatePending() {
            InspTask t = InspTask.create("X", 1L, LocalDate.now());
            assertThat(t.getStatus()).isEqualTo(TaskStatus.PENDING);
            assertThat(t.getInspectorId()).isNull();
            assertThat(t.getRejectionCount()).isEqualTo(0);
            assertThat(t.getDomainEvents()).hasSize(1).first().isInstanceOf(TaskCreatedEvent.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("claim — 领取")
    class ClaimTests {
        @Test
        @DisplayName("PENDING → CLAIMED + 注册 TaskClaimedEvent")
        void shouldClaim() {
            InspTask t = newPending();
            t.claim(10L, "ins");
            assertThat(t.getStatus()).isEqualTo(TaskStatus.CLAIMED);
            assertThat(t.getInspectorId()).isEqualTo(10L);
            assertThat(t.getDomainEvents()).hasSize(1).first().isInstanceOf(TaskClaimedEvent.class);
        }

        @Test
        @DisplayName("CLAIMED → claim 抛 (重复领取)")
        void shouldRejectDoubleClaim() {
            InspTask t = inState(TaskStatus.CLAIMED);
            assertThatThrownBy(() -> t.claim(10L, "ins")).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("IN_PROGRESS → claim 抛")
        void shouldRejectClaimFromInProgress() {
            InspTask t = inState(TaskStatus.IN_PROGRESS);
            assertThatThrownBy(() -> t.claim(10L, "ins")).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("start / submit / startReview / review — 推进")
    class FlowForwardTests {
        @Test
        @DisplayName("CLAIMED → IN_PROGRESS")
        void shouldStart() {
            InspTask t = inState(TaskStatus.CLAIMED);
            t.start();
            assertThat(t.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("IN_PROGRESS → SUBMITTED + 注册 TaskSubmittedEvent + submittedAt 写入")
        void shouldSubmit() {
            InspTask t = inState(TaskStatus.IN_PROGRESS);
            t.submit();
            assertThat(t.getStatus()).isEqualTo(TaskStatus.SUBMITTED);
            assertThat(t.getSubmittedAt()).isNotNull();
            assertThat(t.getDomainEvents()).hasSize(1).first().isInstanceOf(TaskSubmittedEvent.class);
        }

        @Test
        @DisplayName("SUBMITTED → UNDER_REVIEW + reviewerId 写入")
        void shouldStartReview() {
            InspTask t = inState(TaskStatus.SUBMITTED);
            t.startReview(20L, "reviewer");
            assertThat(t.getStatus()).isEqualTo(TaskStatus.UNDER_REVIEW);
            assertThat(t.getReviewerId()).isEqualTo(20L);
        }

        @Test
        @DisplayName("UNDER_REVIEW → REVIEWED + 注册 TaskReviewedEvent")
        void shouldReview() {
            InspTask t = inState(TaskStatus.UNDER_REVIEW);
            t.review("通过");
            assertThat(t.getStatus()).isEqualTo(TaskStatus.REVIEWED);
            assertThat(t.getReviewComment()).isEqualTo("通过");
            assertThat(t.getDomainEvents()).hasSize(1).first().isInstanceOf(TaskReviewedEvent.class);
        }

        @Test
        @DisplayName("PENDING → start 抛 (没领取就不能开始)")
        void shouldRejectStartFromPending() {
            assertThatThrownBy(newPending()::start).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("CLAIMED → submit 抛 (必须先 start)")
        void shouldRejectSubmitFromClaimed() {
            InspTask t = inState(TaskStatus.CLAIMED);
            assertThatThrownBy(t::submit).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("publish — 发布 (自动 / 审核后)")
    class PublishTests {
        @Test
        @DisplayName("REVIEWED → PUBLISHED (标准)")
        void shouldPublishAfterReview() {
            InspTask t = inState(TaskStatus.REVIEWED);
            t.publish(false);
            assertThat(t.getStatus()).isEqualTo(TaskStatus.PUBLISHED);
            assertThat(t.getPublishedAt()).isNotNull();
            assertThat(t.getDomainEvents()).hasSize(1).first().isInstanceOf(TaskPublishedEvent.class);
        }

        @Test
        @DisplayName("SUBMITTED → PUBLISHED (autoPublish=true 跳过审核)")
        void shouldAutoPublishFromSubmitted() {
            InspTask t = inState(TaskStatus.SUBMITTED);
            t.publish(true);
            assertThat(t.getStatus()).isEqualTo(TaskStatus.PUBLISHED);
        }

        @Test
        @DisplayName("SUBMITTED + autoPublish=false → publish 抛 (须先审核)")
        void shouldRejectPublishWhenAutoPublishFalse() {
            InspTask t = inState(TaskStatus.SUBMITTED);
            assertThatThrownBy(() -> t.publish(false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("自动发布");
        }

        @Test
        @DisplayName("IN_PROGRESS → publish 抛")
        void shouldRejectPublishFromInProgress() {
            InspTask t = inState(TaskStatus.IN_PROGRESS);
            assertThatThrownBy(() -> t.publish(true)).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("reject — 驳回 (P1#5: 上限 + 自动延期)")
    class RejectTests {
        @Test
        @DisplayName("SUBMITTED → IN_PROGRESS + rejectionCount=1 + extendedTo 延期 1 天 + 注册 TaskRejectedEvent")
        void shouldRejectFromSubmitted() {
            InspTask t = inState(TaskStatus.SUBMITTED);
            t.reject("整改");
            assertThat(t.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(t.getRejectionCount()).isEqualTo(1);
            assertThat(t.getSubmittedAt()).isNull();
            assertThat(t.getExtendedTo()).isEqualTo(LocalDate.of(2026, 5, 2)); // taskDate + 1
            assertThat(t.getDomainEvents()).hasSize(1).first().isInstanceOf(TaskRejectedEvent.class);
        }

        @Test
        @DisplayName("UNDER_REVIEW → IN_PROGRESS 也允许驳回")
        void shouldRejectFromUnderReview() {
            InspTask t = inState(TaskStatus.UNDER_REVIEW);
            t.reject("整改");
            assertThat(t.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("rejectionCount 已达上限 (默认 3) → 第 4 次抛 IllegalStateException")
        void shouldRejectAtMaxRejectCount() {
            InspTask t = InspTask.builder().id(1L).taskCode("X").projectId(1L)
                    .taskDate(LocalDate.of(2026, 5, 1))
                    .status(TaskStatus.SUBMITTED)
                    .inspectorId(10L).rejectionCount(3).build();
            assertThatThrownBy(() -> t.reject("再驳"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("自动驳回上限");
        }

        @Test
        @DisplayName("项目级 maxRejectCount=5 时, 上限按 5 算")
        void shouldHonorProjectLevelMaxRejectCount() {
            InspTask t = InspTask.builder().id(1L).taskCode("X").projectId(1L)
                    .taskDate(LocalDate.of(2026, 5, 1))
                    .status(TaskStatus.SUBMITTED)
                    .inspectorId(10L).rejectionCount(3).build();
            // 默认上限 3 会抛, 但项目级 5 应该允许
            t.reject("再驳", 5);
            assertThat(t.getRejectionCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("驳回累加延期: 第 2 次驳回基于已延期日期再延 1 天")
        void shouldAccumulateExtension() {
            InspTask t = inState(TaskStatus.SUBMITTED);
            t.reject("once");
            assertThat(t.getExtendedTo()).isEqualTo(LocalDate.of(2026, 5, 2));
            // 模拟用户改完再提交再驳
            t = InspTask.builder().id(t.getId()).taskCode(t.getTaskCode()).projectId(t.getProjectId())
                    .taskDate(t.getTaskDate()).status(TaskStatus.SUBMITTED)
                    .inspectorId(10L).rejectionCount(1).extendedTo(t.getExtendedTo()).build();
            t.reject("twice");
            assertThat(t.getExtendedTo()).isEqualTo(LocalDate.of(2026, 5, 3));
            assertThat(t.getRejectionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("PENDING → reject 抛")
        void shouldRejectRejectionFromPending() {
            assertThatThrownBy(() -> newPending().reject("X")).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("withdraw — 检查员撤回提交")
    class WithdrawTests {
        @Test
        @DisplayName("SUBMITTED → IN_PROGRESS + submittedAt 清空")
        void shouldWithdraw() {
            InspTask t = inState(TaskStatus.SUBMITTED);
            t.withdraw();
            assertThat(t.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(t.getSubmittedAt()).isNull();
        }

        @Test
        @DisplayName("UNDER_REVIEW → withdraw 抛 (已进入审核不可撤回)")
        void shouldRejectWithdrawAfterReviewStarted() {
            InspTask t = inState(TaskStatus.UNDER_REVIEW);
            assertThatThrownBy(t::withdraw).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("unclaim — 离职重派 (review #D)")
    class UnclaimTests {
        @Test
        @DisplayName("CLAIMED → PENDING + 清 inspector + 注册 InspectorUnclaimedEvent")
        void shouldUnclaimFromClaimed() {
            InspTask t = inState(TaskStatus.CLAIMED);
            t.unclaim("离职");
            assertThat(t.getStatus()).isEqualTo(TaskStatus.PENDING);
            assertThat(t.getInspectorId()).isNull();
            assertThat(t.getInspectorName()).isNull();
            assertThat(t.getDomainEvents()).hasSize(1).first().isInstanceOf(InspectorUnclaimedEvent.class);
        }

        @Test
        @DisplayName("IN_PROGRESS → PENDING")
        void shouldUnclaimFromInProgress() {
            InspTask t = inState(TaskStatus.IN_PROGRESS);
            t.unclaim("离职");
            assertThat(t.getStatus()).isEqualTo(TaskStatus.PENDING);
            assertThat(t.getInspectorId()).isNull();
        }

        @Test
        @DisplayName("SUBMITTED → unclaim 抛 (已提交不能撤换检查员)")
        void shouldRejectUnclaimAfterSubmitted() {
            InspTask t = inState(TaskStatus.SUBMITTED);
            assertThatThrownBy(() -> t.unclaim("X")).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("cancel / expire — 终止")
    class TerminateTests {
        @Test
        @DisplayName("PENDING → cancel 成功")
        void shouldCancelPending() {
            InspTask t = newPending();
            t.cancel();
            assertThat(t.getStatus()).isEqualTo(TaskStatus.CANCELLED);
            assertThat(t.getDomainEvents()).hasSize(1).first().isInstanceOf(TaskCancelledEvent.class);
        }

        @Test
        @DisplayName("PUBLISHED → cancel 抛")
        void shouldRejectCancelPublished() {
            InspTask t = inState(TaskStatus.PUBLISHED);
            assertThatThrownBy(t::cancel).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("PENDING → expire 成功")
        void shouldExpirePending() {
            InspTask t = newPending();
            t.expire();
            assertThat(t.getStatus()).isEqualTo(TaskStatus.EXPIRED);
            assertThat(t.getDomainEvents()).hasSize(1).first().isInstanceOf(TaskExpiredEvent.class);
        }

        @Test
        @DisplayName("CLAIMED → expire 成功")
        void shouldExpireClaimed() {
            InspTask t = inState(TaskStatus.CLAIMED);
            t.expire();
            assertThat(t.getStatus()).isEqualTo(TaskStatus.EXPIRED);
        }

        @Test
        @DisplayName("SUBMITTED → expire 抛 (已提交不该过期)")
        void shouldRejectExpireSubmitted() {
            InspTask t = inState(TaskStatus.SUBMITTED);
            assertThatThrownBy(t::expire).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("extendDeadline — 手动延期")
    class ExtendDeadlineTests {
        @Test
        @DisplayName("延期到原 taskDate 之后, extendedTo 写入")
        void shouldExtendBeyondTaskDate() {
            InspTask t = inState(TaskStatus.IN_PROGRESS);
            t.extendDeadline(LocalDate.of(2026, 5, 10));
            assertThat(t.getExtendedTo()).isEqualTo(LocalDate.of(2026, 5, 10));
            assertThat(t.getEffectiveDeadline()).isEqualTo(LocalDate.of(2026, 5, 10));
        }

        @Test
        @DisplayName("已延期后再延期, 必须更晚才接受")
        void shouldRejectEarlierExtension() {
            InspTask t = InspTask.builder().id(1L).taskCode("X").projectId(1L)
                    .taskDate(LocalDate.of(2026, 5, 1)).extendedTo(LocalDate.of(2026, 5, 10))
                    .status(TaskStatus.IN_PROGRESS).inspectorId(10L).build();
            assertThatThrownBy(() -> t.extendDeadline(LocalDate.of(2026, 5, 5)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("extendDeadline(null) 抛")
        void shouldRejectNullDeadline() {
            InspTask t = inState(TaskStatus.IN_PROGRESS);
            assertThatThrownBy(() -> t.extendDeadline(null)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("assign — 项目管理员分派 (PENDING)")
    class AssignTests {
        @Test
        @DisplayName("PENDING → 分派 inspector, 状态保持 PENDING")
        void shouldAssign() {
            InspTask t = newPending();
            t.assign(20L, "ins");
            assertThat(t.getStatus()).isEqualTo(TaskStatus.PENDING);
            assertThat(t.getInspectorId()).isEqualTo(20L);
        }

        @Test
        @DisplayName("CLAIMED → assign 抛")
        void shouldRejectAssignAfterClaim() {
            InspTask t = inState(TaskStatus.CLAIMED);
            assertThatThrownBy(() -> t.assign(20L, "ins")).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("完整生命周期")
    class FullLifecycleTests {
        @Test
        @DisplayName("PENDING → CLAIMED → IN_PROGRESS → SUBMITTED → UNDER_REVIEW → REVIEWED → PUBLISHED")
        void shouldWalkStandardLifecycle() {
            InspTask t = newPending();
            t.claim(10L, "ins");
            t.start();
            t.submit();
            t.startReview(20L, "rev");
            t.review("通过");
            t.publish(false);
            assertThat(t.getStatus()).isEqualTo(TaskStatus.PUBLISHED);
        }

        @Test
        @DisplayName("自动发布捷径: CLAIM → START → SUBMIT → autoPublish=true → PUBLISHED")
        void shouldWalkAutoPublishLifecycle() {
            InspTask t = newPending();
            t.claim(10L, "ins");
            t.start();
            t.submit();
            t.publish(true);
            assertThat(t.getStatus()).isEqualTo(TaskStatus.PUBLISHED);
        }

        @Test
        @DisplayName("驳回回路: SUBMIT → REJECT → IN_PROGRESS → SUBMIT → REVIEW → PUBLISH")
        void shouldHandleRejectAndResubmit() {
            InspTask t = newPending();
            t.claim(10L, "ins");
            t.start();
            t.submit();
            t.reject("整改");
            assertThat(t.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(t.getRejectionCount()).isEqualTo(1);
            t.submit();
            t.startReview(20L, "rev");
            t.review("通过");
            t.publish(false);
            assertThat(t.getStatus()).isEqualTo(TaskStatus.PUBLISHED);
        }

        @Test
        @DisplayName("离职重派: CLAIM → IN_PROGRESS → UNCLAIM → 重新 CLAIM → 完整推进")
        void shouldHandleUnclaimAndReassign() {
            InspTask t = newPending();
            t.claim(10L, "ins1");
            t.start();
            t.unclaim("离职");
            assertThat(t.getStatus()).isEqualTo(TaskStatus.PENDING);
            t.claim(20L, "ins2");
            t.start();
            t.submit();
            assertThat(t.getStatus()).isEqualTo(TaskStatus.SUBMITTED);
            assertThat(t.getInspectorId()).isEqualTo(20L);
        }
    }
}
