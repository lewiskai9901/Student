package com.school.management.application.inspection.evaluation;

import com.school.management.domain.inspection.event.SubmissionCompletedEvent;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.SubmissionStatus;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.model.scoring.TriggerMode;
import com.school.management.domain.inspection.repository.IndicatorRepository;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.List;

/**
 * 计数阈值触发器 (Phase 3, 2026-05-23).
 *
 * <p>监听 {@link SubmissionCompletedEvent}; 对每个 {@code triggerMode==COUNT} 的 indicator:
 * <ol>
 *   <li>累计该 target 在该 indicator sourceSection 上的 COMPLETED submission 数</li>
 *   <li>若 {@code count % countThreshold == 0} → 触发评估, periodKey="COUNT#"+(count/threshold)</li>
 * </ol>
 *
 * <p>事务约定 (memory 铁律):
 * {@code @TransactionalEventListener(AFTER_COMMIT) + @Transactional(REQUIRES_NEW)}.
 * 跨 service 边界 listener 不可用 @EventListener (会读到未提交状态),
 * 也不可用默认 @Transactional 传播 (commit 后无现成事务可加入).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CountThresholdTrigger {

    private final IndicatorRepository indicatorRepository;
    private final InspTaskRepository taskRepository;
    private final InspSubmissionRepository submissionRepository;
    private final IndicatorEvaluationService evaluationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSubmissionCompleted(SubmissionCompletedEvent event) {
        if (event == null || event.getSubmissionId() == null) return;

        InspSubmission sub = submissionRepository.findById(event.getSubmissionId()).orElse(null);
        if (sub == null || sub.getSectionId() == null || sub.getTargetId() == null) return;

        InspTask task = taskRepository.findById(sub.getTaskId()).orElse(null);
        if (task == null) return;

        List<Indicator> projectIndicators = indicatorRepository.findByProjectId(task.getProjectId());
        for (Indicator ind : projectIndicators) {
            if (ind.getTriggerMode() != TriggerMode.COUNT) continue;
            if (ind.getCountThreshold() == null || ind.getCountThreshold() < 1) continue;
            if (!ind.getSourceSectionIds().contains(sub.getSectionId())) continue;

            int count = countCompletedSubmissionsFor(ind, sub.getTargetId());
            if (count == 0) continue;
            if (count % ind.getCountThreshold() != 0) continue;

            int bucket = count / ind.getCountThreshold();
            String periodKey = PeriodKeyResolver.forCount(bucket);

            // COUNT 模式的 period 边界用项目全期 (用最早完成日 → 当前 submission 完成日)
            LocalDate start = LocalDate.of(1970, 1, 1);
            LocalDate end = LocalDate.now();
            try {
                evaluationService.evaluate(ind.getId(), periodKey, start, end);
                log.info("CountThresholdTrigger 触发 indicator={} target={} count={} bucket={}",
                        ind.getId(), sub.getTargetId(), count, bucket);
            } catch (Exception ex) {
                log.error("CountThresholdTrigger 评估失败 indicator={}: {}",
                        ind.getId(), ex.getMessage(), ex);
            }
        }
    }

    private int countCompletedSubmissionsFor(Indicator indicator, Long targetId) {
        // 简化: 拉项目全部 task → submission, 过滤 (target, sectionIds, COMPLETED).
        // 真生产建议加 mapper count 端点; Phase 3 先用现有 repo 接口.
        int count = 0;
        List<InspTask> tasks = taskRepository.findByProjectId(indicator.getProjectId());
        for (InspTask task : tasks) {
            List<InspSubmission> subs = submissionRepository.findByTaskId(task.getId());
            for (InspSubmission s : subs) {
                if (s.getStatus() != SubmissionStatus.COMPLETED) continue;
                if (!targetId.equals(s.getTargetId())) continue;
                if (s.getSectionId() == null) continue;
                if (!indicator.getSourceSectionIds().contains(s.getSectionId())) continue;
                count++;
            }
        }
        return count;
    }
}
