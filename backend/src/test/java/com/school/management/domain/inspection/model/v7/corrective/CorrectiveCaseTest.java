package com.school.management.domain.inspection.model.v7.corrective;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CorrectiveCase 聚合根测试")
class CorrectiveCaseTest {

    // ==================== 工厂方法 ====================

    private CorrectiveCase createOpenCase() {
        return CorrectiveCase.create("CASE-001", "卫生不达标", CasePriority.MEDIUM, 1L);
    }

    private CorrectiveCase createAssignedCase() {
        CorrectiveCase c = createOpenCase();
        c.assign(10L, "张三");
        return c;
    }

    private CorrectiveCase createInProgressCase() {
        CorrectiveCase c = createAssignedCase();
        c.startWork();
        return c;
    }

    private CorrectiveCase createSubmittedCase() {
        CorrectiveCase c = createInProgressCase();
        c.submitCorrection("已整改完成", List.of(100L, 101L));
        return c;
    }

    private CorrectiveCase createVerifiedCase() {
        CorrectiveCase c = createSubmittedCase();
        c.verify(20L, "李四", "验证通过");
        return c;
    }

    private CorrectiveCase createClosedCase() {
        CorrectiveCase c = createVerifiedCase();
        c.close();
        return c;
    }

    // ==================== 创建测试 ====================

    @Nested
    @DisplayName("创建案例")
    class CreateTests {

        @Test
        @DisplayName("新建案例初始状态为OPEN")
        void testCreate() {
            // When
            CorrectiveCase c = CorrectiveCase.create(
                    "CASE-001", "走廊有垃圾", CasePriority.HIGH, 1L);

            // Then
            assertThat(c.getStatus()).isEqualTo(CaseStatus.OPEN);
            assertThat(c.getCaseCode()).isEqualTo("CASE-001");
            assertThat(c.getIssueDescription()).isEqualTo("走廊有垃圾");
            assertThat(c.getPriority()).isEqualTo(CasePriority.HIGH);
            assertThat(c.getCreatedBy()).isEqualTo(1L);
            assertThat(c.getEscalationLevel()).isEqualTo(0);
            assertThat(c.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("新建案例默认优先级为MEDIUM")
        void testCreate_DefaultPriority() {
            CorrectiveCase c = CorrectiveCase.create(
                    "CASE-002", "问题描述", null, 1L);

            assertThat(c.getPriority()).isEqualTo(CasePriority.MEDIUM);
        }
    }

    // ==================== 分配测试 ====================

    @Nested
    @DisplayName("分配责任人")
    class AssignTests {

        @Test
        @DisplayName("OPEN → ASSIGNED 正常分配")
        void testAssign() {
            // Given
            CorrectiveCase c = createOpenCase();

            // When
            c.assign(10L, "张三");

            // Then
            assertThat(c.getStatus()).isEqualTo(CaseStatus.ASSIGNED);
            assertThat(c.getAssigneeId()).isEqualTo(10L);
            assertThat(c.getAssigneeName()).isEqualTo("张三");
            assertThat(c.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("REJECTED → ASSIGNED 驳回后重新分配")
        void testAssign_AfterRejection() {
            // Given: 走完整流程到 REJECTED
            CorrectiveCase c = createSubmittedCase();
            c.reject(20L, "李四", "整改不到位");

            assertThat(c.getStatus()).isEqualTo(CaseStatus.REJECTED);

            // When: 重新分配
            c.assign(30L, "王五");

            // Then
            assertThat(c.getStatus()).isEqualTo(CaseStatus.ASSIGNED);
            assertThat(c.getAssigneeId()).isEqualTo(30L);
        }

        @Test
        @DisplayName("非OPEN/REJECTED状态分配应抛异常")
        void testAssign_InvalidState() {
            CorrectiveCase c = createAssignedCase();

            assertThatThrownBy(() -> c.assign(20L, "李四"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有待分配或被驳回的案例才能分配责任人");
        }
    }

    // ==================== 提交整改测试 ====================

    @Nested
    @DisplayName("提交整改")
    class SubmitCorrectionTests {

        @Test
        @DisplayName("IN_PROGRESS → SUBMITTED 正常提交")
        void testSubmitCorrection() {
            // Given
            CorrectiveCase c = createInProgressCase();

            // When
            c.submitCorrection("已完成整改", List.of(100L));

            // Then
            assertThat(c.getStatus()).isEqualTo(CaseStatus.SUBMITTED);
            assertThat(c.getCorrectionNote()).isEqualTo("已完成整改");
            assertThat(c.getCorrectionEvidenceIds()).containsExactly(100L);
            assertThat(c.getCorrectedAt()).isNotNull();
        }

        @Test
        @DisplayName("非IN_PROGRESS状态提交应抛异常")
        void testSubmitCorrection_InvalidState() {
            CorrectiveCase c = createAssignedCase();

            assertThatThrownBy(() -> c.submitCorrection("测试", List.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有进行中的案例才能提交整改");
        }
    }

    // ==================== 验证测试 ====================

    @Nested
    @DisplayName("验证")
    class VerifyTests {

        @Test
        @DisplayName("SUBMITTED → VERIFIED 验证通过")
        void testVerify_Accepted() {
            // Given
            CorrectiveCase c = createSubmittedCase();

            // When
            c.verify(20L, "李四", "整改到位，验证通过");

            // Then
            assertThat(c.getStatus()).isEqualTo(CaseStatus.VERIFIED);
            assertThat(c.getVerifierId()).isEqualTo(20L);
            assertThat(c.getVerifierName()).isEqualTo("李四");
            assertThat(c.getVerificationNote()).isEqualTo("整改到位，验证通过");
            assertThat(c.getVerifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("SUBMITTED → REJECTED 验证驳回")
        void testVerify_Rejected() {
            // Given
            CorrectiveCase c = createSubmittedCase();

            // When
            c.reject(20L, "李四", "整改不到位，需重新整改");

            // Then
            assertThat(c.getStatus()).isEqualTo(CaseStatus.REJECTED);
            assertThat(c.getVerifierId()).isEqualTo(20L);
            assertThat(c.getVerificationNote()).isEqualTo("整改不到位，需重新整改");
        }

        @Test
        @DisplayName("非SUBMITTED状态验证应抛异常")
        void testVerify_InvalidState() {
            CorrectiveCase c = createInProgressCase();

            assertThatThrownBy(() -> c.verify(20L, "李四", "测试"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已提交整改的案例才能验证");
        }

        @Test
        @DisplayName("非SUBMITTED状态驳回应抛异常")
        void testReject_InvalidState() {
            CorrectiveCase c = createInProgressCase();

            assertThatThrownBy(() -> c.reject(20L, "李四", "理由"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已提交整改的案例才能驳回");
        }
    }

    // ==================== 关闭测试 ====================

    @Nested
    @DisplayName("关闭案例")
    class CloseTests {

        @Test
        @DisplayName("VERIFIED → CLOSED 关闭并设置效果验证")
        void testClose() {
            // Given
            CorrectiveCase c = createVerifiedCase();

            // When
            c.close();

            // Then
            assertThat(c.getStatus()).isEqualTo(CaseStatus.CLOSED);
            assertThat(c.getEffectivenessStatus()).isEqualTo(EffectivenessStatus.PENDING);
            assertThat(c.getEffectivenessCheckDate()).isNotNull();
        }

        @Test
        @DisplayName("非VERIFIED状态关闭应抛异常")
        void testClose_InvalidState() {
            CorrectiveCase c = createSubmittedCase();

            assertThatThrownBy(() -> c.close())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已验证的案例才能关闭");
        }
    }

    // ==================== 效果验证测试 ====================

    @Nested
    @DisplayName("效果验证")
    class EffectivenessTests {

        @Test
        @DisplayName("效果验证通过")
        void testConfirmEffectiveness() {
            // Given
            CorrectiveCase c = createClosedCase();

            // When
            c.confirmEffectiveness("效果良好，未再出现类似问题");

            // Then
            assertThat(c.getEffectivenessStatus()).isEqualTo(EffectivenessStatus.CONFIRMED);
            assertThat(c.getEffectivenessNote()).isEqualTo("效果良好，未再出现类似问题");
        }

        @Test
        @DisplayName("效果验证不达标 — 重新打开并升级")
        void testFailEffectiveness() {
            // Given
            CorrectiveCase c = createClosedCase();
            int originalLevel = c.getEscalationLevel();

            // When
            c.failEffectiveness("问题重现，需重新整改");

            // Then
            assertThat(c.getStatus()).isEqualTo(CaseStatus.OPEN);
            assertThat(c.getEffectivenessStatus()).isEqualTo(EffectivenessStatus.FAILED);
            assertThat(c.getEscalationLevel()).isEqualTo(originalLevel + 1);
        }
    }

    // ==================== 升级测试 ====================

    @Nested
    @DisplayName("案例升级")
    class EscalateTests {

        @Test
        @DisplayName("升级 — 增加升级等级并重置为OPEN")
        void testEscalate() {
            // Given
            CorrectiveCase c = createAssignedCase();
            int originalLevel = c.getEscalationLevel();

            // When
            c.escalate();

            // Then: 升级后回到 OPEN，等待重新分配给更高级别责任人
            assertThat(c.getStatus()).isEqualTo(CaseStatus.OPEN);
            assertThat(c.getEscalationLevel()).isEqualTo(originalLevel + 1);
            assertThat(c.getAssigneeId()).isNull();
            assertThat(c.getAssigneeName()).isNull();
        }

        @Test
        @DisplayName("升级可以多次执行（每次递增 escalationLevel）")
        void testEscalate_MultipleTimes() {
            // Given
            CorrectiveCase c = createOpenCase();

            // When: 连续升级
            c.escalate();
            assertThat(c.getEscalationLevel()).isEqualTo(1);
            assertThat(c.getStatus()).isEqualTo(CaseStatus.OPEN);

            c.escalate();
            assertThat(c.getEscalationLevel()).isEqualTo(2);
            assertThat(c.getStatus()).isEqualTo(CaseStatus.OPEN);
        }

        @Test
        @DisplayName("已关闭案例不能升级")
        void testEscalate_ClosedThrows() {
            CorrectiveCase c = createClosedCase();

            assertThatThrownBy(() -> c.escalate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("已关闭的案例不能升级");
        }

        @Test
        @DisplayName("升级后可以重新分配")
        void testEscalate_ThenReassign() {
            // Given: 已分配的案例
            CorrectiveCase c = createAssignedCase();

            // When: 升级
            c.escalate();
            assertThat(c.getStatus()).isEqualTo(CaseStatus.OPEN);

            // Then: 可以重新分配给更高级别责任人
            c.assign(30L, "主管");
            assertThat(c.getStatus()).isEqualTo(CaseStatus.ASSIGNED);
            assertThat(c.getAssigneeId()).isEqualTo(30L);
            assertThat(c.getEscalationLevel()).isEqualTo(1);
        }
    }

    // ==================== 非法状态转换测试 ====================

    @Nested
    @DisplayName("非法状态转换")
    class InvalidTransitionTests {

        @Test
        @DisplayName("OPEN → CLOSED 应抛异常（跳过中间状态）")
        void testInvalidTransition_OpenToClosedThrows() {
            CorrectiveCase c = createOpenCase();

            assertThatThrownBy(() -> c.close())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("OPEN → SUBMITTED 应抛异常")
        void testInvalidTransition_OpenToSubmittedThrows() {
            CorrectiveCase c = createOpenCase();

            assertThatThrownBy(() -> c.submitCorrection("测试", List.of()))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("ASSIGNED → VERIFIED 应抛异常")
        void testInvalidTransition_AssignedToVerifiedThrows() {
            CorrectiveCase c = createAssignedCase();

            assertThatThrownBy(() -> c.verify(20L, "李四", "测试"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("OPEN → startWork 应抛异常（需要先 assign）")
        void testInvalidTransition_OpenToInProgressThrows() {
            CorrectiveCase c = createOpenCase();

            assertThatThrownBy(() -> c.startWork())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已分配的案例才能开始整改");
        }
    }

    // ==================== 根因分析和预防措施测试 ====================

    @Nested
    @DisplayName("根因分析和预防措施")
    class RcaAndPreventiveTests {

        @Test
        @DisplayName("设置根因分析")
        void testSetRootCauseAnalysis() {
            CorrectiveCase c = createOpenCase();

            c.setRootCauseAnalysis(RcaMethod.FIVE_WHYS, "{\"whys\": [\"why1\", \"why2\"]}");

            assertThat(c.getRcaMethod()).isEqualTo(RcaMethod.FIVE_WHYS);
            assertThat(c.getRcaData()).contains("why1");
        }

        @Test
        @DisplayName("设置预防措施")
        void testSetPreventiveAction() {
            CorrectiveCase c = createOpenCase();

            c.setPreventiveAction("加强日常巡检频率");

            assertThat(c.getPreventiveAction()).isEqualTo("加强日常巡检频率");
        }
    }

    // ==================== SLA 测试 ====================

    @Nested
    @DisplayName("SLA 超时")
    class SlaTests {

        @Test
        @DisplayName("SLA超时自动升级")
        void testSlaBreach() {
            CorrectiveCase c = createAssignedCase();
            int originalLevel = c.getEscalationLevel();

            c.slaBreach();

            assertThat(c.getEscalationLevel()).isEqualTo(originalLevel + 1);
        }
    }
}
