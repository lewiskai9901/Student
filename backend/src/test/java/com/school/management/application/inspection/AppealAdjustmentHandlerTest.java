package com.school.management.application.inspection;

import com.school.management.domain.inspection.event.AppealApprovedEvent;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import com.school.management.domain.inspection.repository.SubmissionDetailRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AppealAdjustmentHandler 单元测试 (I7 / I3 follow-up).
 *
 * <p>验证 I3 的关键修复:
 * <ul>
 *   <li>申诉通过事件 → detail 应用调整 + save</li>
 *   <li>调用 recalculateFromSubmission(submissionId) — **不是**整项目当日全扫</li>
 *   <li>detail 不存在 → 不抛, 跳过</li>
 *   <li>调整异常 → 不再触发后续重算 (避免脏数据扩散)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppealAdjustmentHandler — 申诉通过副作用")
class AppealAdjustmentHandlerTest {

    @Mock SubmissionDetailRepository detailRepository;
    @Mock ScoreAggregationService scoreAggregationService;
    @InjectMocks AppealAdjustmentHandler handler;

    @Test
    @DisplayName("正常路径 — apply 调整 + save + 单 submission 重算")
    void happyPath_appliesAdjustmentAndCascades() {
        SubmissionDetail detail = mockDetail(50L, 100L);
        when(detailRepository.findById(50L)).thenReturn(Optional.of(detail));

        AppealApprovedEvent event = new AppealApprovedEvent(
                7L, "APL-001", 50L, 999L, BigDecimal.valueOf(1.5));
        handler.handle(event);

        verify(detail).applyAppealAdjustment(BigDecimal.valueOf(1.5));
        verify(detailRepository).save(detail);
        // I3: 单 submission 重算, 不是整项目当日全扫
        verify(scoreAggregationService).recalculateFromSubmission(100L);
    }

    @Test
    @DisplayName("detail 不存在 → 不抛, 不调 score 重算")
    void detailNotFound_skipsSilently() {
        when(detailRepository.findById(99L)).thenReturn(Optional.empty());

        AppealApprovedEvent event = new AppealApprovedEvent(
                7L, "APL-002", 99L, 999L, BigDecimal.valueOf(1.0));
        handler.handle(event);

        verify(scoreAggregationService, never()).recalculateFromSubmission(any());
    }

    @Test
    @DisplayName("event 缺 submissionDetailId → 不处理")
    void missingDetailId_skipsSilently() {
        AppealApprovedEvent event = new AppealApprovedEvent(
                7L, "APL-003", null, 999L, BigDecimal.valueOf(1.0));
        handler.handle(event);

        verify(detailRepository, never()).findById(any());
        verify(scoreAggregationService, never()).recalculateFromSubmission(any());
    }

    @Test
    @DisplayName("apply 调整异常 → 不调用 score 重算 (避免脏数据)")
    void applyFails_doesNotCascade() {
        SubmissionDetail detail = mockDetail(60L, 200L);
        when(detailRepository.findById(60L)).thenReturn(Optional.of(detail));
        org.mockito.Mockito.doThrow(new IllegalStateException("can't adjust"))
                .when(detail).applyAppealAdjustment(any());

        AppealApprovedEvent event = new AppealApprovedEvent(
                7L, "APL-004", 60L, 999L, BigDecimal.valueOf(2.0));
        handler.handle(event);

        verify(scoreAggregationService, never()).recalculateFromSubmission(any());
    }

    @Test
    @DisplayName("submissionId 缺失 → 不级联重算")
    void missingSubmissionId_skipsCascade() {
        SubmissionDetail detail = mockDetail(70L, null);
        when(detailRepository.findById(70L)).thenReturn(Optional.of(detail));

        AppealApprovedEvent event = new AppealApprovedEvent(
                7L, "APL-005", 70L, 999L, BigDecimal.valueOf(0.5));
        handler.handle(event);

        verify(detailRepository).save(detail);   // detail save 仍发生
        verify(scoreAggregationService, never()).recalculateFromSubmission(any());
    }

    private SubmissionDetail mockDetail(Long detailId, Long submissionId) {
        SubmissionDetail d = org.mockito.Mockito.mock(SubmissionDetail.class);
        org.mockito.Mockito.lenient().when(d.getSubmissionId()).thenReturn(submissionId);
        return d;
    }
}
