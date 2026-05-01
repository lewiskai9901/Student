package com.school.management.domain.inspection.model.corrective;

import com.school.management.domain.inspection.event.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CorrectiveCase 整改案例聚合根单测.
 *
 * 状态机:
 *   OPEN → ASSIGNED → IN_PROGRESS → SUBMITTED → VERIFIED → CLOSED
 *                                              ↓
 *                                           REJECTED → ASSIGNED (再分配)
 *
 * 核心规则:
 *   - escalate / slaBreach / failEffectiveness 受 MAX_AUTO_ESCALATION_LEVEL=3 保护
 *   - close 时按 priority 自动设置 effectivenessCheckDate
 *   - unassign (P1#6 离职重派): ASSIGNED/IN_PROGRESS → OPEN
 */
@DisplayName("CorrectiveCase 整改案例聚合根")
class CorrectiveCaseTest {

    private static CorrectiveCase newOpen() {
        CorrectiveCase c = CorrectiveCase.create("CASE-T-001", "卫生不合格", CasePriority.HIGH, 999L);
        c.clearDomainEvents();
        return c;
    }

    private static CorrectiveCase inState(CaseStatus status) {
        CorrectiveCase.Builder b = CorrectiveCase.builder()
                .id(1L).caseCode("CASE-T-001")
                .issueDescription("卫生不合格")
                .priority(CasePriority.HIGH)
                .status(status).createdBy(999L)
                .escalationLevel(0);
        if (status == CaseStatus.ASSIGNED || status == CaseStatus.IN_PROGRESS
                || status == CaseStatus.SUBMITTED || status == CaseStatus.VERIFIED
                || status == CaseStatus.CLOSED || status == CaseStatus.REJECTED) {
            b.assigneeId(10L).assigneeName("assignee");
        }
        if (status == CaseStatus.CLOSED) {
            b.effectivenessStatus(EffectivenessStatus.PENDING);
        }
        return b.build();
    }

    @Nested
    @DisplayName("create — 工厂")
    class CreateTests {
        @Test
        @DisplayName("创建后 OPEN + 注册 CorrectiveCaseCreatedEvent")
        void shouldCreateOpen() {
            CorrectiveCase c = CorrectiveCase.create("X", "issue", CasePriority.MEDIUM, 999L);
            assertThat(c.getStatus()).isEqualTo(CaseStatus.OPEN);
            assertThat(c.getEscalationLevel()).isEqualTo(0);
            assertThat(c.getDomainEvents()).hasSize(1).first().isInstanceOf(CorrectiveCaseCreatedEvent.class);
        }
    }

    @Nested
    @DisplayName("assign — 分配责任人")
    class AssignTests {
        @Test
        @DisplayName("OPEN → ASSIGNED + 注册 CaseAssignedEvent")
        void shouldAssignFromOpen() {
            CorrectiveCase c = newOpen();
            c.assign(10L, "assignee");
            assertThat(c.getStatus()).isEqualTo(CaseStatus.ASSIGNED);
            assertThat(c.getAssigneeId()).isEqualTo(10L);
            assertThat(c.getDomainEvents()).hasSize(1).first().isInstanceOf(CaseAssignedEvent.class);
        }

        @Test
        @DisplayName("REJECTED → ASSIGNED 再分配")
        void shouldAssignFromRejected() {
            CorrectiveCase c = inState(CaseStatus.REJECTED);
            c.assign(20L, "newAssignee");
            assertThat(c.getStatus()).isEqualTo(CaseStatus.ASSIGNED);
            assertThat(c.getAssigneeId()).isEqualTo(20L);
        }

        @Test
        @DisplayName("ASSIGNED → assign 抛 (重复分配)")
        void shouldRejectDoubleAssign() {
            CorrectiveCase c = inState(CaseStatus.ASSIGNED);
            assertThatThrownBy(() -> c.assign(20L, "X"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("CLOSED → assign 抛")
        void shouldRejectAssignClosed() {
            CorrectiveCase c = inState(CaseStatus.CLOSED);
            assertThatThrownBy(() -> c.assign(20L, "X"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("unassign — 离职重派 (P1#6)")
    class UnassignTests {
        @Test
        @DisplayName("ASSIGNED → OPEN + 清 assignee + 注册 AssigneeUnassignedEvent")
        void shouldUnassignFromAssigned() {
            CorrectiveCase c = inState(CaseStatus.ASSIGNED);
            c.unassign("离职");
            assertThat(c.getStatus()).isEqualTo(CaseStatus.OPEN);
            assertThat(c.getAssigneeId()).isNull();
            assertThat(c.getDomainEvents()).hasSize(1).first().isInstanceOf(AssigneeUnassignedEvent.class);
        }

        @Test
        @DisplayName("IN_PROGRESS → OPEN")
        void shouldUnassignFromInProgress() {
            CorrectiveCase c = inState(CaseStatus.IN_PROGRESS);
            c.unassign("离职");
            assertThat(c.getStatus()).isEqualTo(CaseStatus.OPEN);
        }

        @Test
        @DisplayName("SUBMITTED → unassign 抛 (已提交不能撤换)")
        void shouldRejectUnassignAfterSubmitted() {
            CorrectiveCase c = inState(CaseStatus.SUBMITTED);
            assertThatThrownBy(() -> c.unassign("X"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("startWork / submitCorrection / verify / reject — 整改链路")
    class WorkflowTests {
        @Test
        @DisplayName("ASSIGNED → IN_PROGRESS")
        void shouldStartWork() {
            CorrectiveCase c = inState(CaseStatus.ASSIGNED);
            c.startWork();
            assertThat(c.getStatus()).isEqualTo(CaseStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("IN_PROGRESS → SUBMITTED + 注册 CorrectionSubmittedEvent + 写入 correctedAt/note")
        void shouldSubmitCorrection() {
            CorrectiveCase c = inState(CaseStatus.IN_PROGRESS);
            c.submitCorrection("已修复", List.of(1L, 2L));
            assertThat(c.getStatus()).isEqualTo(CaseStatus.SUBMITTED);
            assertThat(c.getCorrectionNote()).isEqualTo("已修复");
            assertThat(c.getCorrectedAt()).isNotNull();
            assertThat(c.getDomainEvents()).hasSize(1).first().isInstanceOf(CorrectionSubmittedEvent.class);
        }

        @Test
        @DisplayName("SUBMITTED → VERIFIED + 注册 CaseVerifiedEvent")
        void shouldVerify() {
            CorrectiveCase c = inState(CaseStatus.SUBMITTED);
            c.verify(20L, "verifier", "确认整改");
            assertThat(c.getStatus()).isEqualTo(CaseStatus.VERIFIED);
            assertThat(c.getVerifierId()).isEqualTo(20L);
            assertThat(c.getDomainEvents()).hasSize(1).first().isInstanceOf(CaseVerifiedEvent.class);
        }

        @Test
        @DisplayName("SUBMITTED → REJECTED + 注册 CaseRejectedEvent")
        void shouldReject() {
            CorrectiveCase c = inState(CaseStatus.SUBMITTED);
            c.reject(20L, "verifier", "未达标");
            assertThat(c.getStatus()).isEqualTo(CaseStatus.REJECTED);
            assertThat(c.getDomainEvents()).hasSize(1).first().isInstanceOf(CaseRejectedEvent.class);
        }

        @Test
        @DisplayName("OPEN → submitCorrection 抛")
        void shouldRejectSubmitFromOpen() {
            CorrectiveCase c = newOpen();
            assertThatThrownBy(() -> c.submitCorrection("X", List.of()))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("VERIFIED → verify 抛 (重复验证)")
        void shouldRejectDoubleVerify() {
            CorrectiveCase c = inState(CaseStatus.VERIFIED);
            assertThatThrownBy(() -> c.verify(20L, "X", "X"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("close — 关闭 + 自动设置效果验证日期")
    class CloseTests {
        @Test
        @DisplayName("VERIFIED → CLOSED + effectivenessStatus=PENDING + 14 天 effectivenessCheckDate")
        void shouldCloseAndSetEffectivenessCheck() {
            CorrectiveCase c = inState(CaseStatus.VERIFIED);
            c.close();
            assertThat(c.getStatus()).isEqualTo(CaseStatus.CLOSED);
            assertThat(c.getEffectivenessStatus()).isEqualTo(EffectivenessStatus.PENDING);
            assertThat(c.getEffectivenessCheckDate()).isNotNull();
            assertThat(c.getDomainEvents()).hasSize(1).first().isInstanceOf(CaseClosedEvent.class);
        }

        @Test
        @DisplayName("OPEN → close 抛")
        void shouldRejectCloseFromOpen() {
            CorrectiveCase c = newOpen();
            assertThatThrownBy(c::close).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("confirmEffectiveness / failEffectiveness — 效果验证")
    class EffectivenessTests {
        @Test
        @DisplayName("CLOSED + PENDING → confirmEffectiveness 写 CONFIRMED")
        void shouldConfirmEffectiveness() {
            CorrectiveCase c = inState(CaseStatus.CLOSED);
            c.confirmEffectiveness("效果良好");
            assertThat(c.getEffectivenessStatus()).isEqualTo(EffectivenessStatus.CONFIRMED);
            assertThat(c.getDomainEvents()).hasSize(1).first().isInstanceOf(EffectivenessConfirmedEvent.class);
        }

        @Test
        @DisplayName("CLOSED + PENDING → failEffectiveness 重新打开为 OPEN + escalationLevel+1")
        void shouldReopenWhenEffectivenessFails() {
            CorrectiveCase c = inState(CaseStatus.CLOSED);
            c.failEffectiveness("反弹");
            assertThat(c.getStatus()).isEqualTo(CaseStatus.OPEN);
            assertThat(c.getEscalationLevel()).isEqualTo(1);
            assertThat(c.getEffectivenessStatus()).isEqualTo(EffectivenessStatus.FAILED);
        }

        @Test
        @DisplayName("escalationLevel 已达 3 时 failEffectiveness 不再 reopen")
        void shouldNotReopenWhenAtMaxLevel() {
            CorrectiveCase c = CorrectiveCase.builder()
                    .id(1L).caseCode("X").issueDescription("X").priority(CasePriority.HIGH)
                    .status(CaseStatus.CLOSED).effectivenessStatus(EffectivenessStatus.PENDING)
                    .escalationLevel(3).build();
            c.failEffectiveness("再次反弹");
            assertThat(c.getStatus()).isEqualTo(CaseStatus.CLOSED);
            assertThat(c.getEffectivenessStatus()).isEqualTo(EffectivenessStatus.FAILED);
            assertThat(c.getEscalationLevel()).isEqualTo(3);
            assertThat(c.getDomainEvents()).hasSize(1).first().isInstanceOf(EffectivenessFailedEvent.class);
        }

        @Test
        @DisplayName("项目级 maxEscalationLevel=5 允许第 4 次升级")
        void shouldHonorProjectLevelMaxEscalation() {
            CorrectiveCase c = CorrectiveCase.builder()
                    .id(1L).caseCode("X").issueDescription("X").priority(CasePriority.HIGH)
                    .status(CaseStatus.CLOSED).effectivenessStatus(EffectivenessStatus.PENDING)
                    .escalationLevel(3).build();
            c.failEffectiveness("再次反弹", 5);
            assertThat(c.getStatus()).isEqualTo(CaseStatus.OPEN);
            assertThat(c.getEscalationLevel()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("escalate — 主动升级")
    class EscalateTests {
        @Test
        @DisplayName("IN_PROGRESS → escalate: 重置为 OPEN + 清 assignee + level+1")
        void shouldEscalate() {
            CorrectiveCase c = inState(CaseStatus.IN_PROGRESS);
            c.escalate();
            assertThat(c.getStatus()).isEqualTo(CaseStatus.OPEN);
            assertThat(c.getAssigneeId()).isNull();
            assertThat(c.getEscalationLevel()).isEqualTo(1);
            assertThat(c.getDomainEvents()).hasSize(1).first().isInstanceOf(CaseEscalatedEvent.class);
        }

        @Test
        @DisplayName("CLOSED → escalate 抛")
        void shouldRejectEscalateClosed() {
            CorrectiveCase c = inState(CaseStatus.CLOSED);
            assertThatThrownBy(c::escalate).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("escalationLevel=3 → escalate 抛 (须人工介入)")
        void shouldRejectEscalateAtMaxLevel() {
            CorrectiveCase c = CorrectiveCase.builder()
                    .id(1L).caseCode("X").issueDescription("X").priority(CasePriority.HIGH)
                    .status(CaseStatus.IN_PROGRESS).escalationLevel(3).build();
            assertThatThrownBy(c::escalate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("自动升级上限");
        }
    }

    @Nested
    @DisplayName("slaBreach — SLA 超时升级")
    class SlaBreachTests {
        @Test
        @DisplayName("level=0 → slaBreach: level=1 + 注册 SlaBreachedEvent")
        void shouldEscalateOnSlaBreach() {
            CorrectiveCase c = inState(CaseStatus.IN_PROGRESS);
            c.slaBreach();
            assertThat(c.getEscalationLevel()).isEqualTo(1);
            assertThat(c.getDomainEvents()).hasSize(1).first().isInstanceOf(SlaBreachedEvent.class);
        }

        @Test
        @DisplayName("level=3 → slaBreach 不加 level, 仍发事件")
        void shouldNotEscalatePastMaxOnSlaBreach() {
            CorrectiveCase c = CorrectiveCase.builder()
                    .id(1L).caseCode("X").issueDescription("X").priority(CasePriority.HIGH)
                    .status(CaseStatus.IN_PROGRESS).escalationLevel(3).build();
            c.slaBreach();
            assertThat(c.getEscalationLevel()).isEqualTo(3);
            assertThat(c.getDomainEvents()).hasSize(1).first().isInstanceOf(SlaBreachedEvent.class);
        }
    }

    @Nested
    @DisplayName("完整生命周期")
    class FullLifecycleTests {
        @Test
        @DisplayName("OPEN → ASSIGNED → IN_PROGRESS → SUBMITTED → VERIFIED → CLOSED")
        void shouldWalkStandardLifecycle() {
            CorrectiveCase c = newOpen();
            c.assign(10L, "ass");
            c.startWork();
            c.submitCorrection("done", List.of());
            c.verify(20L, "ver", "ok");
            c.close();
            assertThat(c.getStatus()).isEqualTo(CaseStatus.CLOSED);
            assertThat(c.getEffectivenessStatus()).isEqualTo(EffectivenessStatus.PENDING);
        }

        @Test
        @DisplayName("驳回回路: SUBMITTED → REJECTED → ASSIGNED → ... → CLOSED")
        void shouldHandleRejectAndResubmit() {
            CorrectiveCase c = newOpen();
            c.assign(10L, "ass");
            c.startWork();
            c.submitCorrection("done", List.of());
            c.reject(20L, "ver", "no");
            assertThat(c.getStatus()).isEqualTo(CaseStatus.REJECTED);
            c.assign(10L, "ass");
            c.startWork();
            c.submitCorrection("redo", List.of());
            c.verify(20L, "ver", "ok");
            c.close();
            assertThat(c.getStatus()).isEqualTo(CaseStatus.CLOSED);
        }
    }
}
