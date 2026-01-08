package com.school.management.domain.inspection.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 申诉聚合根单元测试
 * 测试申诉状态机的完整流程
 */
@DisplayName("申诉聚合根测试")
class AppealTest {

    private Appeal appeal;
    private static final Long INSPECTION_RECORD_ID = 1L;
    private static final Long DEDUCTION_DETAIL_ID = 2L;
    private static final Long CLASS_ID = 3L;
    private static final Long APPLICANT_ID = 100L;
    private static final Long REVIEWER_1_ID = 200L;
    private static final Long REVIEWER_2_ID = 300L;

    @BeforeEach
    void setUp() {
        appeal = createValidAppeal();
    }

    private Appeal createValidAppeal() {
        return Appeal.create(
                INSPECTION_RECORD_ID,
                DEDUCTION_DETAIL_ID,
                CLASS_ID,
                "APP-20260103-001",
                "扣分有误，实际情况与检查记录不符",
                Arrays.asList("evidence1.jpg", "evidence2.jpg"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                APPLICANT_ID
        );
    }

    @Nested
    @DisplayName("创建申诉测试")
    class CreateAppealTest {

        @Test
        @DisplayName("成功创建申诉")
        void shouldCreateAppealSuccessfully() {
            assertNotNull(appeal);
            assertEquals(INSPECTION_RECORD_ID, appeal.getInspectionRecordId());
            assertEquals(DEDUCTION_DETAIL_ID, appeal.getDeductionDetailId());
            assertEquals(CLASS_ID, appeal.getClassId());
            assertEquals(APPLICANT_ID, appeal.getApplicantId());
            assertEquals(AppealStatus.PENDING, appeal.getStatus());
            assertNotNull(appeal.getAppliedAt());
            assertEquals(2, appeal.getAttachments().size());
        }

        @Test
        @DisplayName("创建申诉时检查必填字段 - 缺少检查记录ID")
        void shouldFailWhenInspectionRecordIdIsNull() {
            assertThrows(IllegalArgumentException.class, () -> {
                Appeal.create(null, DEDUCTION_DETAIL_ID, CLASS_ID,
                        "APP-001", "理由", null,
                        new BigDecimal("10"), new BigDecimal("5"), APPLICANT_ID);
            });
        }

        @Test
        @DisplayName("创建申诉时检查必填字段 - 缺少申诉理由")
        void shouldFailWhenReasonIsEmpty() {
            assertThrows(IllegalArgumentException.class, () -> {
                Appeal.create(INSPECTION_RECORD_ID, DEDUCTION_DETAIL_ID, CLASS_ID,
                        "APP-001", "", null,
                        new BigDecimal("10"), new BigDecimal("5"), APPLICANT_ID);
            });
        }

        @Test
        @DisplayName("创建申诉时检查必填字段 - 缺少原扣分")
        void shouldFailWhenOriginalDeductionIsNull() {
            assertThrows(IllegalArgumentException.class, () -> {
                Appeal.create(INSPECTION_RECORD_ID, DEDUCTION_DETAIL_ID, CLASS_ID,
                        "APP-001", "理由", null,
                        null, new BigDecimal("5"), APPLICANT_ID);
            });
        }
    }

    @Nested
    @DisplayName("一级审核测试")
    class Level1ReviewTest {

        @Test
        @DisplayName("开始一级审核")
        void shouldStartLevel1Review() {
            appeal.startLevel1Review(REVIEWER_1_ID);

            assertEquals(AppealStatus.LEVEL1_REVIEWING, appeal.getStatus());
            assertEquals(REVIEWER_1_ID, appeal.getLevel1ReviewerId());
            assertEquals(1, appeal.getApprovalRecords().size());
            assertFalse(appeal.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("一级审核通过")
        void shouldLevel1Approve() {
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Approve(REVIEWER_1_ID, "情况属实，同意申诉");

            assertEquals(AppealStatus.LEVEL1_APPROVED, appeal.getStatus());
            assertEquals("情况属实，同意申诉", appeal.getLevel1Comment());
            assertNotNull(appeal.getLevel1ReviewedAt());
        }

        @Test
        @DisplayName("一级审核驳回")
        void shouldLevel1Reject() {
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Reject(REVIEWER_1_ID, "证据不足，驳回申诉");

            assertEquals(AppealStatus.LEVEL1_REJECTED, appeal.getStatus());
            assertEquals("证据不足，驳回申诉", appeal.getLevel1Comment());
        }

        @Test
        @DisplayName("非法状态转换 - 从PENDING直接到LEVEL1_APPROVED")
        void shouldFailDirectTransitionToLevel1Approved() {
            assertThrows(IllegalStateException.class, () -> {
                appeal.level1Approve(REVIEWER_1_ID, "comment");
            });
        }
    }

    @Nested
    @DisplayName("二级审核测试")
    class Level2ReviewTest {

        @BeforeEach
        void passLevel1() {
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Approve(REVIEWER_1_ID, "同意");
        }

        @Test
        @DisplayName("开始二级审核")
        void shouldStartLevel2Review() {
            appeal.startLevel2Review(REVIEWER_2_ID);

            assertEquals(AppealStatus.LEVEL2_REVIEWING, appeal.getStatus());
            assertEquals(REVIEWER_2_ID, appeal.getLevel2ReviewerId());
        }

        @Test
        @DisplayName("二级审核通过（最终审批）")
        void shouldFinalApprove() {
            appeal.startLevel2Review(REVIEWER_2_ID);
            BigDecimal approvedDeduction = new BigDecimal("3.00");
            appeal.approve(REVIEWER_2_ID, "最终批准", approvedDeduction);

            assertEquals(AppealStatus.APPROVED, appeal.getStatus());
            assertEquals(approvedDeduction, appeal.getApprovedDeduction());
            assertEquals("最终批准", appeal.getLevel2Comment());
        }

        @Test
        @DisplayName("二级审核驳回")
        void shouldFinalReject() {
            appeal.startLevel2Review(REVIEWER_2_ID);
            appeal.reject(REVIEWER_2_ID, "经复核，原检查结果正确");

            assertEquals(AppealStatus.REJECTED, appeal.getStatus());
        }
    }

    @Nested
    @DisplayName("撤回申诉测试")
    class WithdrawTest {

        @Test
        @DisplayName("申请人可以撤回待处理的申诉")
        void shouldWithdrawPendingAppeal() {
            appeal.withdraw(APPLICANT_ID);

            assertEquals(AppealStatus.WITHDRAWN, appeal.getStatus());
        }

        @Test
        @DisplayName("非申请人不能撤回申诉")
        void shouldFailWhenNonApplicantWithdraws() {
            assertThrows(IllegalArgumentException.class, () -> {
                appeal.withdraw(999L);
            });
        }

        @Test
        @DisplayName("非PENDING状态不能撤回")
        void shouldFailWhenNotPending() {
            appeal.startLevel1Review(REVIEWER_1_ID);

            assertThrows(IllegalStateException.class, () -> {
                appeal.withdraw(APPLICANT_ID);
            });
        }
    }

    @Nested
    @DisplayName("申诉生效测试")
    class MakeEffectiveTest {

        @BeforeEach
        void approveAppeal() {
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Approve(REVIEWER_1_ID, "同意");
            appeal.startLevel2Review(REVIEWER_2_ID);
            appeal.approve(REVIEWER_2_ID, "批准", new BigDecimal("3.00"));
        }

        @Test
        @DisplayName("批准的申诉可以生效")
        void shouldMakeEffective() {
            appeal.makeEffective();

            assertEquals(AppealStatus.EFFECTIVE, appeal.getStatus());
            assertNotNull(appeal.getEffectiveAt());
        }

        @Test
        @DisplayName("计算分数差异")
        void shouldCalculateScoreDifference() {
            BigDecimal difference = appeal.calculateScoreDifference();

            // 原扣分10 - 批准扣分3 = 7
            assertEquals(new BigDecimal("7.00"), difference);
        }
    }

    @Nested
    @DisplayName("完整审批流程测试")
    class FullWorkflowTest {

        @Test
        @DisplayName("完整的申诉通过流程")
        void shouldCompleteFullApprovalWorkflow() {
            // 初始状态
            assertEquals(AppealStatus.PENDING, appeal.getStatus());

            // 一级审核
            appeal.startLevel1Review(REVIEWER_1_ID);
            assertEquals(AppealStatus.LEVEL1_REVIEWING, appeal.getStatus());

            appeal.level1Approve(REVIEWER_1_ID, "初审通过");
            assertEquals(AppealStatus.LEVEL1_APPROVED, appeal.getStatus());

            // 二级审核
            appeal.startLevel2Review(REVIEWER_2_ID);
            assertEquals(AppealStatus.LEVEL2_REVIEWING, appeal.getStatus());

            appeal.approve(REVIEWER_2_ID, "终审通过", new BigDecimal("2.00"));
            assertEquals(AppealStatus.APPROVED, appeal.getStatus());

            // 生效
            appeal.makeEffective();
            assertEquals(AppealStatus.EFFECTIVE, appeal.getStatus());

            // 验证审批记录
            assertEquals(5, appeal.getApprovalRecords().size());
        }

        @Test
        @DisplayName("一级驳回终止流程")
        void shouldTerminateOnLevel1Rejection() {
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Reject(REVIEWER_1_ID, "驳回");

            assertEquals(AppealStatus.LEVEL1_REJECTED, appeal.getStatus());

            // 不能继续二级审核
            assertThrows(IllegalStateException.class, () -> {
                appeal.startLevel2Review(REVIEWER_2_ID);
            });
        }
    }

    @Nested
    @DisplayName("领域事件测试")
    class DomainEventTest {

        @Test
        @DisplayName("状态变更时发布领域事件")
        void shouldPublishEventOnStatusChange() {
            appeal.startLevel1Review(REVIEWER_1_ID);

            assertFalse(appeal.getDomainEvents().isEmpty());
            assertTrue(appeal.getDomainEvents().stream()
                    .anyMatch(e -> e instanceof com.school.management.domain.inspection.event.AppealStatusChangedEvent));
        }

        @Test
        @DisplayName("清除事件后列表为空")
        void shouldClearEvents() {
            appeal.startLevel1Review(REVIEWER_1_ID);
            assertFalse(appeal.getDomainEvents().isEmpty());

            appeal.clearDomainEvents();
            assertTrue(appeal.getDomainEvents().isEmpty());
        }
    }
}
