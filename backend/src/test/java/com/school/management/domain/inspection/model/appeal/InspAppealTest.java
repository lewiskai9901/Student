package com.school.management.domain.inspection.model.appeal;

import com.school.management.domain.inspection.event.AppealApprovedEvent;
import com.school.management.domain.inspection.event.AppealRejectedEvent;
import com.school.management.domain.inspection.event.AppealSubmittedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * InspAppeal 申诉聚合根单测.
 *
 * 状态机: PENDING → APPROVED / REJECTED / WITHDRAWN
 */
@DisplayName("InspAppeal 申诉聚合根")
class InspAppealTest {

    private static InspAppeal newPending() {
        InspAppeal a = InspAppeal.submit(
                "APL-T-001", 100L, 200L, 300L, 400L,
                "STUDENT", 500L,
                999L, "submitter",
                "扣分依据不清", null, BigDecimal.valueOf(2.0));
        a.clearDomainEvents();
        return a;
    }

    private static InspAppeal inState(AppealStatus status) {
        return InspAppeal.builder()
                .id(1L).appealCode("APL-T-001")
                .submissionDetailId(100L).submissionId(200L)
                .taskId(300L).projectId(400L)
                .subjectType("STUDENT").subjectId(500L)
                .submitterUserId(999L).submitterName("submitter")
                .reason("扣分依据不清")
                .status(status).build();
    }

    // ============================================================
    @Nested
    @DisplayName("submit — 工厂")
    class SubmitTests {
        @Test
        @DisplayName("正常提交后 PENDING + 注册 AppealSubmittedEvent")
        void shouldSubmit() {
            InspAppeal a = InspAppeal.submit(
                    "APL-T-002", 100L, 200L, 300L, 400L,
                    "STUDENT", 500L, 999L, "X", "理由", null, BigDecimal.ONE);
            assertThat(a.getStatus()).isEqualTo(AppealStatus.PENDING);
            assertThat(a.getDomainEvents()).hasSize(1).first().isInstanceOf(AppealSubmittedEvent.class);
        }

        @Test
        @DisplayName("appealCode 为空抛 IllegalArgumentException")
        void shouldRejectBlankCode() {
            assertThatThrownBy(() -> InspAppeal.submit(
                    "  ", 100L, 200L, 300L, 400L,
                    "STUDENT", 500L, 999L, "X", "理由", null, BigDecimal.ONE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("申诉编号");
        }

        @Test
        @DisplayName("submissionDetailId=null 抛")
        void shouldRejectNullDetailId() {
            assertThatThrownBy(() -> InspAppeal.submit(
                    "APL", null, 200L, 300L, 400L,
                    "STUDENT", 500L, 999L, "X", "理由", null, BigDecimal.ONE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("扣分项");
        }

        @Test
        @DisplayName("submitterUserId=null 抛")
        void shouldRejectNullSubmitterId() {
            assertThatThrownBy(() -> InspAppeal.submit(
                    "APL", 100L, 200L, 300L, 400L,
                    "STUDENT", 500L, null, "X", "理由", null, BigDecimal.ONE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("reason 空白抛")
        void shouldRejectBlankReason() {
            assertThatThrownBy(() -> InspAppeal.submit(
                    "APL", 100L, 200L, 300L, 400L,
                    "STUDENT", 500L, 999L, "X", "  ", null, BigDecimal.ONE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("理由");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("approve — 审核通过")
    class ApproveTests {
        @Test
        @DisplayName("PENDING → APPROVED + finalAdjustment 写入 + 事件")
        void shouldApprove() {
            InspAppeal a = newPending();
            a.approve(20L, "reviewer", "属实", BigDecimal.valueOf(3.0));
            assertThat(a.getStatus()).isEqualTo(AppealStatus.APPROVED);
            assertThat(a.getReviewerId()).isEqualTo(20L);
            assertThat(a.getFinalAdjustment()).isEqualByComparingTo(BigDecimal.valueOf(3.0));
            assertThat(a.getReviewedAt()).isNotNull();
            assertThat(a.getDomainEvents()).hasSize(1).first().isInstanceOf(AppealApprovedEvent.class);
        }

        @Test
        @DisplayName("APPROVED → approve 抛 (重复)")
        void shouldRejectDoubleApprove() {
            InspAppeal a = inState(AppealStatus.APPROVED);
            assertThatThrownBy(() -> a.approve(20L, "X", "X", BigDecimal.ZERO))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("REJECTED → approve 抛")
        void shouldRejectApproveAfterReject() {
            InspAppeal a = inState(AppealStatus.REJECTED);
            assertThatThrownBy(() -> a.approve(20L, "X", "X", BigDecimal.ZERO))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("reject — 审核驳回")
    class RejectTests {
        @Test
        @DisplayName("PENDING → REJECTED + comment 写入 + 事件")
        void shouldReject() {
            InspAppeal a = newPending();
            a.reject(20L, "reviewer", "证据不足");
            assertThat(a.getStatus()).isEqualTo(AppealStatus.REJECTED);
            assertThat(a.getReviewerComment()).isEqualTo("证据不足");
            assertThat(a.getDomainEvents()).hasSize(1).first().isInstanceOf(AppealRejectedEvent.class);
        }

        @Test
        @DisplayName("驳回 comment 空白抛 IllegalArgumentException")
        void shouldRejectBlankComment() {
            InspAppeal a = newPending();
            assertThatThrownBy(() -> a.reject(20L, "X", "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("APPROVED → reject 抛")
        void shouldRejectRejectAfterApprove() {
            InspAppeal a = inState(AppealStatus.APPROVED);
            assertThatThrownBy(() -> a.reject(20L, "X", "X"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("WITHDRAWN → reject 抛")
        void shouldRejectRejectAfterWithdraw() {
            InspAppeal a = inState(AppealStatus.WITHDRAWN);
            assertThatThrownBy(() -> a.reject(20L, "X", "X"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("withdraw — 提交人撤回")
    class WithdrawTests {
        @Test
        @DisplayName("PENDING + 提交人本人 → WITHDRAWN")
        void shouldWithdrawByOwner() {
            InspAppeal a = newPending();
            a.withdraw(999L);
            assertThat(a.getStatus()).isEqualTo(AppealStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("非提交人撤回抛 IllegalStateException")
        void shouldRejectWithdrawByOthers() {
            InspAppeal a = newPending();
            assertThatThrownBy(() -> a.withdraw(888L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("仅申诉提交人");
        }

        @Test
        @DisplayName("APPROVED → withdraw 抛 (审核已完成)")
        void shouldRejectWithdrawAfterApprove() {
            InspAppeal a = inState(AppealStatus.APPROVED);
            assertThatThrownBy(() -> a.withdraw(999L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("byUserId=null 抛")
        void shouldRejectNullUserId() {
            InspAppeal a = newPending();
            assertThatThrownBy(() -> a.withdraw(null))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
