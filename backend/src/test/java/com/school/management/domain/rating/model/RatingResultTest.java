package com.school.management.domain.rating.model;

import com.school.management.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rating结果聚合根单元测试
 */
@DisplayName("评比结果聚合根测试")
class RatingResultTest {

    private static final Long RATING_CONFIG_ID = 1L;
    private static final Long CHECK_PLAN_ID = 10L;
    private static final Long CLASS_ID = 100L;
    private static final String CLASS_NAME = "高一(1)班";
    private static final Long REVIEWER_ID = 200L;
    private static final Long PUBLISHER_ID = 201L;

    private RatingResult result;

    @BeforeEach
    void setUp() {
        result = RatingResult.create(
            RATING_CONFIG_ID,
            CHECK_PLAN_ID,
            CLASS_ID,
            CLASS_NAME,
            RatingPeriodType.WEEKLY,
            LocalDate.now().minusDays(7),
            LocalDate.now()
        );
    }

    @Nested
    @DisplayName("创建结果测试")
    class CreateResultTest {

        @Test
        @DisplayName("成功创建评比结果")
        void shouldCreateResultSuccessfully() {
            assertNotNull(result);
            assertEquals(RATING_CONFIG_ID, result.getRatingConfigId());
            assertEquals(CHECK_PLAN_ID, result.getCheckPlanId());
            assertEquals(CLASS_ID, result.getClassId());
            assertEquals(CLASS_NAME, result.getClassName());
            assertEquals(RatingPeriodType.WEEKLY, result.getPeriodType());
            assertEquals(RatingResultStatus.DRAFT, result.getStatus());
            assertFalse(result.isAwarded());
        }
    }

    @Nested
    @DisplayName("计算结果测试")
    class CalculateResultTest {

        @Test
        @DisplayName("更新计算结果")
        void shouldUpdateCalculation() {
            result.updateCalculation(1, BigDecimal.valueOf(95.5), true);

            assertEquals(1, result.getRanking());
            assertEquals(BigDecimal.valueOf(95.5), result.getFinalScore());
            assertTrue(result.isAwarded());
            assertNotNull(result.getCalculatedAt());
        }

        @Test
        @DisplayName("未获奖结果")
        void shouldUpdateNonAwardedResult() {
            result.updateCalculation(15, BigDecimal.valueOf(75.0), false);

            assertEquals(15, result.getRanking());
            assertFalse(result.isAwarded());
        }
    }

    @Nested
    @DisplayName("状态流转测试")
    class StatusTransitionTest {

        @Test
        @DisplayName("提交审核")
        void shouldSubmitForApproval() {
            result.clearDomainEvents();

            result.submitForApproval(REVIEWER_ID);

            assertEquals(RatingResultStatus.PENDING_APPROVAL, result.getStatus());
            assertFalse(result.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("审核通过")
        void shouldApprove() {
            result.submitForApproval(REVIEWER_ID);
            result.clearDomainEvents();

            result.approve(REVIEWER_ID);

            assertEquals(RatingResultStatus.APPROVED, result.getStatus());
            assertEquals(REVIEWER_ID, result.getApprovedBy());
            assertNotNull(result.getApprovedAt());
        }

        @Test
        @DisplayName("审核驳回")
        void shouldReject() {
            result.submitForApproval(REVIEWER_ID);
            result.clearDomainEvents();

            result.reject(REVIEWER_ID, "数据有误");

            assertEquals(RatingResultStatus.REJECTED, result.getStatus());
        }

        @Test
        @DisplayName("发布结果")
        void shouldPublish() {
            result.submitForApproval(REVIEWER_ID);
            result.approve(REVIEWER_ID);
            result.clearDomainEvents();

            result.publish(PUBLISHER_ID);

            assertEquals(RatingResultStatus.PUBLISHED, result.getStatus());
            assertEquals(PUBLISHER_ID, result.getPublishedBy());
            assertNotNull(result.getPublishedAt());
        }

        @Test
        @DisplayName("撤销发布")
        void shouldRevoke() {
            result.submitForApproval(REVIEWER_ID);
            result.approve(REVIEWER_ID);
            result.publish(PUBLISHER_ID);
            result.clearDomainEvents();

            result.revoke(PUBLISHER_ID, "发现数据错误");

            assertEquals(RatingResultStatus.REVOKED, result.getStatus());
        }

        @Test
        @DisplayName("DRAFT状态不能直接发布")
        void shouldFailToPublishFromDraft() {
            assertThrows(BusinessException.class, () ->
                result.publish(PUBLISHER_ID)
            );
        }

        @Test
        @DisplayName("PENDING_APPROVAL状态不能直接发布")
        void shouldFailToPublishFromPending() {
            result.submitForApproval(REVIEWER_ID);

            assertThrows(BusinessException.class, () ->
                result.publish(PUBLISHER_ID)
            );
        }

        @Test
        @DisplayName("REVOKED状态不能再撤销")
        void shouldFailToRevokeFromRevoked() {
            result.submitForApproval(REVIEWER_ID);
            result.approve(REVIEWER_ID);
            result.publish(PUBLISHER_ID);
            result.revoke(PUBLISHER_ID, "reason");

            assertThrows(BusinessException.class, () ->
                result.revoke(PUBLISHER_ID, "reason2")
            );
        }
    }

    @Nested
    @DisplayName("允许的状态转换测试")
    class AllowedTransitionsTest {

        @Test
        @DisplayName("DRAFT允许提交")
        void draftAllowsSubmit() {
            assertTrue(result.getAllowedTransitions().contains(RatingResultStatus.PENDING_APPROVAL));
        }

        @Test
        @DisplayName("PENDING_APPROVAL允许审核和驳回")
        void pendingAllowsApproveAndReject() {
            result.submitForApproval(REVIEWER_ID);

            var transitions = result.getAllowedTransitions();
            assertTrue(transitions.contains(RatingResultStatus.APPROVED));
            assertTrue(transitions.contains(RatingResultStatus.REJECTED));
        }

        @Test
        @DisplayName("APPROVED允许发布")
        void approvedAllowsPublish() {
            result.submitForApproval(REVIEWER_ID);
            result.approve(REVIEWER_ID);

            assertTrue(result.getAllowedTransitions().contains(RatingResultStatus.PUBLISHED));
        }

        @Test
        @DisplayName("PUBLISHED允许撤销")
        void publishedAllowsRevoke() {
            result.submitForApproval(REVIEWER_ID);
            result.approve(REVIEWER_ID);
            result.publish(PUBLISHER_ID);

            assertTrue(result.getAllowedTransitions().contains(RatingResultStatus.REVOKED));
        }
    }

    @Nested
    @DisplayName("完整工作流测试")
    class FullWorkflowTest {

        @Test
        @DisplayName("完整审批发布流程")
        void shouldCompleteFullWorkflow() {
            // 计算
            result.updateCalculation(1, BigDecimal.valueOf(98.5), true);
            assertEquals(RatingResultStatus.DRAFT, result.getStatus());

            // 提交
            result.submitForApproval(REVIEWER_ID);
            assertEquals(RatingResultStatus.PENDING_APPROVAL, result.getStatus());

            // 审核
            result.approve(REVIEWER_ID);
            assertEquals(RatingResultStatus.APPROVED, result.getStatus());

            // 发布
            result.publish(PUBLISHER_ID);
            assertEquals(RatingResultStatus.PUBLISHED, result.getStatus());
        }

        @Test
        @DisplayName("驳回后重新提交流程")
        void shouldCompleteResubmitWorkflow() {
            // 提交 -> 驳回
            result.submitForApproval(REVIEWER_ID);
            result.reject(REVIEWER_ID, "数据错误");
            assertEquals(RatingResultStatus.REJECTED, result.getStatus());

            // 重新提交
            result.resubmit(REVIEWER_ID);
            assertEquals(RatingResultStatus.PENDING_APPROVAL, result.getStatus());

            // 审核通过 -> 发布
            result.approve(REVIEWER_ID);
            result.publish(PUBLISHER_ID);
            assertEquals(RatingResultStatus.PUBLISHED, result.getStatus());
        }
    }
}
