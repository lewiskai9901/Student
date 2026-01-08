package com.school.management.domain.inspection.model;

import com.school.management.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 检查记录聚合根单元测试
 */
@DisplayName("检查记录聚合根测试")
class InspectionRecordTest {

    private static final Long TEMPLATE_ID = 1L;
    private static final Long CLASS_ID = 100L;
    private static final String CLASS_NAME = "高一(1)班";
    private static final Long INSPECTOR_ID = 200L;
    private static final String INSPECTOR_NAME = "检查员";
    private static final BigDecimal BASE_SCORE = BigDecimal.valueOf(100);

    private InspectionRecord record;

    @BeforeEach
    void setUp() {
        record = InspectionRecord.create(
            TEMPLATE_ID,
            CLASS_ID,
            CLASS_NAME,
            LocalDate.now(),
            INSPECTOR_ID,
            INSPECTOR_NAME,
            BASE_SCORE
        );
    }

    @Nested
    @DisplayName("创建记录测试")
    class CreateRecordTest {

        @Test
        @DisplayName("成功创建检查记录")
        void shouldCreateRecordSuccessfully() {
            assertNotNull(record);
            assertEquals(TEMPLATE_ID, record.getTemplateId());
            assertEquals(CLASS_ID, record.getClassId());
            assertEquals(CLASS_NAME, record.getClassName());
            assertEquals(INSPECTOR_ID, record.getInspectorId());
            assertEquals(BASE_SCORE, record.getBaseScore());
            assertEquals(InspectionRecordStatus.DRAFT, record.getStatus());
            assertNotNull(record.getInspectionDate());
        }

        @Test
        @DisplayName("初始分数等于基础分")
        void shouldInitializeFinalScoreAsBaseScore() {
            assertEquals(BASE_SCORE, record.getFinalScore());
        }
    }

    @Nested
    @DisplayName("扣分项测试")
    class DeductionItemTest {

        @Test
        @DisplayName("添加扣分项")
        void shouldAddDeductionItem() {
            record.addDeductionItem(1L, "迟到", BigDecimal.valueOf(5), 2, "2人迟到");

            assertEquals(1, record.getDeductionItems().size());
            // 扣分: 5 * 2 = 10
            assertEquals(BigDecimal.valueOf(90), record.getFinalScore());
        }

        @Test
        @DisplayName("多个扣分项累计")
        void shouldAccumulateDeductions() {
            record.addDeductionItem(1L, "迟到", BigDecimal.valueOf(5), 2, null);
            record.addDeductionItem(2L, "卫生不合格", BigDecimal.valueOf(10), 1, null);

            assertEquals(2, record.getDeductionItems().size());
            // 100 - (5*2) - (10*1) = 80
            assertEquals(BigDecimal.valueOf(80), record.getFinalScore());
        }

        @Test
        @DisplayName("删除扣分项")
        void shouldRemoveDeductionItem() {
            record.addDeductionItem(1L, "迟到", BigDecimal.valueOf(5), 2, null);
            Long itemId = record.getDeductionItems().get(0).getId();

            record.removeDeductionItem(itemId);

            assertEquals(0, record.getDeductionItems().size());
            assertEquals(BASE_SCORE, record.getFinalScore());
        }

        @Test
        @DisplayName("最低分数不低于0")
        void shouldNotGoBelowZero() {
            record.addDeductionItem(1L, "严重违规", BigDecimal.valueOf(200), 1, null);

            assertEquals(BigDecimal.ZERO, record.getFinalScore());
        }
    }

    @Nested
    @DisplayName("状态流转测试")
    class StatusTransitionTest {

        @Test
        @DisplayName("提交审核")
        void shouldSubmitForReview() {
            record.submitForReview();

            assertEquals(InspectionRecordStatus.PENDING_REVIEW, record.getStatus());
        }

        @Test
        @DisplayName("审核通过")
        void shouldApprove() {
            record.submitForReview();
            record.approve(300L, "审核员");

            assertEquals(InspectionRecordStatus.APPROVED, record.getStatus());
            assertEquals(300L, record.getReviewerId());
        }

        @Test
        @DisplayName("审核驳回")
        void shouldReject() {
            record.submitForReview();
            record.reject(300L, "审核员", "数据有误");

            assertEquals(InspectionRecordStatus.REJECTED, record.getStatus());
        }

        @Test
        @DisplayName("发布记录")
        void shouldPublish() {
            record.submitForReview();
            record.approve(300L, "审核员");
            record.publish(300L);

            assertEquals(InspectionRecordStatus.PUBLISHED, record.getStatus());
            assertNotNull(record.getPublishedAt());
        }

        @Test
        @DisplayName("DRAFT状态不能直接发布")
        void shouldNotPublishFromDraft() {
            assertThrows(BusinessException.class, () -> record.publish(300L));
        }

        @Test
        @DisplayName("已发布不能再修改扣分项")
        void shouldNotModifyAfterPublished() {
            record.submitForReview();
            record.approve(300L, "审核员");
            record.publish(300L);

            assertThrows(BusinessException.class, () ->
                record.addDeductionItem(1L, "迟到", BigDecimal.valueOf(5), 1, null)
            );
        }
    }

    @Nested
    @DisplayName("业务判断测试")
    class BusinessLogicTest {

        @Test
        @DisplayName("判断是否可编辑")
        void shouldCheckEditable() {
            assertTrue(record.isEditable());

            record.submitForReview();
            assertFalse(record.isEditable());
        }

        @Test
        @DisplayName("判断是否已发布")
        void shouldCheckPublished() {
            assertFalse(record.isPublished());

            record.submitForReview();
            record.approve(300L, "审核员");
            record.publish(300L);

            assertTrue(record.isPublished());
        }

        @Test
        @DisplayName("计算扣分总计")
        void shouldCalculateTotalDeduction() {
            record.addDeductionItem(1L, "迟到", BigDecimal.valueOf(5), 2, null);
            record.addDeductionItem(2L, "卫生", BigDecimal.valueOf(10), 1, null);

            assertEquals(BigDecimal.valueOf(20), record.getTotalDeduction());
        }
    }

    @Nested
    @DisplayName("完整工作流测试")
    class FullWorkflowTest {

        @Test
        @DisplayName("完整检查发布流程")
        void shouldCompleteFullWorkflow() {
            // 创建
            assertEquals(InspectionRecordStatus.DRAFT, record.getStatus());

            // 添加扣分
            record.addDeductionItem(1L, "迟到", BigDecimal.valueOf(5), 1, "1人迟到");
            assertEquals(BigDecimal.valueOf(95), record.getFinalScore());

            // 提交审核
            record.submitForReview();
            assertEquals(InspectionRecordStatus.PENDING_REVIEW, record.getStatus());

            // 审核通过
            record.approve(300L, "审核员");
            assertEquals(InspectionRecordStatus.APPROVED, record.getStatus());

            // 发布
            record.publish(300L);
            assertEquals(InspectionRecordStatus.PUBLISHED, record.getStatus());
        }

        @Test
        @DisplayName("驳回后修改重新提交流程")
        void shouldResubmitAfterRejection() {
            // 提交 -> 驳回
            record.submitForReview();
            record.reject(300L, "审核员", "数据有误");
            assertEquals(InspectionRecordStatus.REJECTED, record.getStatus());

            // 修改
            record.revise();
            assertEquals(InspectionRecordStatus.DRAFT, record.getStatus());

            // 添加/修改扣分项
            record.addDeductionItem(1L, "迟到", BigDecimal.valueOf(3), 1, null);

            // 重新提交
            record.submitForReview();
            record.approve(300L, "审核员");
            record.publish(300L);

            assertEquals(InspectionRecordStatus.PUBLISHED, record.getStatus());
        }
    }
}
