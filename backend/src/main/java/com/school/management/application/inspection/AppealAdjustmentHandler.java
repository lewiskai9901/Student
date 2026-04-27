package com.school.management.application.inspection;

import com.school.management.domain.inspection.event.AppealApprovedEvent;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.SubmissionDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 申诉审核通过后的副作用处理 (P1#8 follow-up — 修复申诉成空头支票的问题).
 *
 * <p>监听 {@link AppealApprovedEvent}, 把 finalAdjustment 真正回填到原 SubmissionDetail
 * 并触发项目分数重算, 让申诉决议在数据上落地.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppealAdjustmentHandler {

    private final SubmissionDetailRepository detailRepository;
    private final InspSubmissionRepository submissionRepository;
    private final InspTaskRepository taskRepository;
    private final ScoreAggregationService scoreAggregationService;

    @Async
    @EventListener
    @Transactional
    public void handle(AppealApprovedEvent event) {
        Long detailId = event.getSubmissionDetailId();
        if (detailId == null) {
            log.warn("AppealApprovedEvent missing submissionDetailId, skip adjustment");
            return;
        }

        SubmissionDetail detail = detailRepository.findById(detailId).orElse(null);
        if (detail == null) {
            log.warn("AppealApproved: submissionDetail {} not found, skip", detailId);
            return;
        }

        try {
            detail.applyAppealAdjustment(event.getFinalAdjustment());
            detailRepository.save(detail);
            log.info("AppealApproved: detailId={} score adjusted to {} (appealCode={})",
                    detailId, detail.getScore(), event.getAppealCode());
        } catch (Exception e) {
            log.error("AppealApproved: 调整 detail {} 分数失败: {}", detailId, e.getMessage(), e);
            return;
        }

        // 联动重算项目当日分数
        try {
            InspSubmission submission = submissionRepository.findById(detail.getSubmissionId()).orElse(null);
            if (submission == null || submission.getTaskId() == null) return;
            InspTask task = taskRepository.findById(submission.getTaskId()).orElse(null);
            if (task == null) return;
            scoreAggregationService.recomputeProjectScore(task.getProjectId(), task.getTaskDate());
            log.info("AppealApproved: recomputed project score projectId={} date={}",
                    task.getProjectId(), task.getTaskDate());
        } catch (Exception e) {
            log.warn("AppealApproved: 项目分数重算失败: {}", e.getMessage());
        }
    }
}
