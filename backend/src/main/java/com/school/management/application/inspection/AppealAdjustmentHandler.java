package com.school.management.application.inspection;

import com.school.management.domain.inspection.event.AppealApprovedEvent;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import com.school.management.domain.inspection.repository.SubmissionDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 申诉审核通过后的副作用处理 (P1#8 follow-up — 修复申诉成空头支票的问题).
 *
 * <p>监听 {@link AppealApprovedEvent}, 把 finalAdjustment 真正回填到原 SubmissionDetail
 * 并触发**单条 submission 级联重算**, 让申诉决议在数据上真正落地.
 *
 * <p>I3 (2026-05-17): 之前直接调 recomputeProjectScore 既粗暴 (整项目当日全扫) 又无效
 * (该方法只 avg submission.finalScore, 不读取 detail.score → 调整未被反映).
 * 改为调 {@code ScoreAggregationService.recalculateFromSubmission(submissionId)}:
 * <ol>
 *   <li>读取 submission 所有 details (含被改的)</li>
 *   <li>重新计算 submission.finalScore</li>
 *   <li>updateTaskCompletedCount</li>
 *   <li>recomputeProjectScore (仅 1 次, 当日)</li>
 * </ol>
 * 工作量从 O(项目当日 task 数 × submission 数 × detail 数) 降到 O(本 submission 的 detail 数).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppealAdjustmentHandler {

    private final SubmissionDetailRepository detailRepository;
    private final ScoreAggregationService scoreAggregationService;

    /**
     * review #2: 改用 AFTER_COMMIT 阶段 — 主事务提交后才触发, 避免读到未提交的 detail/submission 状态.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

        // I3: 走单条 submission 级联重算 — 真正读 detail 重算 submission.finalScore,
        // 顺带 task / project 级别更新, 不再做"整项目当日全扫"
        try {
            if (detail.getSubmissionId() == null) return;
            scoreAggregationService.recalculateFromSubmission(detail.getSubmissionId());
            log.info("AppealApproved: cascade-recalculated submission {} after detail {} adjustment",
                    detail.getSubmissionId(), detailId);
        } catch (Exception e) {
            log.warn("AppealApproved: submission {} 级联重算失败: {}", detail.getSubmissionId(), e.getMessage());
        }
    }
}
