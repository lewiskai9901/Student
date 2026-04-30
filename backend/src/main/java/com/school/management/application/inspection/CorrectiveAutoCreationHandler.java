package com.school.management.application.inspection;

import com.school.management.domain.inspection.event.AutoCorrectiveCreationFailedEvent;
import com.school.management.domain.inspection.event.SubmissionCompletedEvent;
import com.school.management.domain.inspection.model.corrective.CasePriority;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.SubmissionDetailRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自动整改创建处理器
 * 监听 SubmissionCompletedEvent，为标记为问题的明细项自动创建整改案例.
 *
 * <p><b>可靠性保障 (P0#4 + 后续修复):</b>
 * 单次整改单创建失败时通过 {@link TaskScheduler} 延迟调度重试 (1s/2s/4s, 共 3 次).
 * 不再阻塞 @Async 线程, 不嵌套事务边界 — 每次重试是独立的调度任务和事务.
 * 用尽仍失败 → 发出 {@link AutoCorrectiveCreationFailedEvent} 让监控告警感知.
 * 整批次内一个 detail 失败不影响其他 detail 创建.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorrectiveAutoCreationHandler {

    private final SubmissionDetailRepository detailRepository;
    private final InspSubmissionRepository submissionRepository;
    private final InspTaskRepository taskRepository;
    private final CorrectiveCaseApplicationService caseService;
    private final SpringDomainEventPublisher eventPublisher;
    private final TaskScheduler taskScheduler;

    private static final AtomicLong CODE_SEQUENCE = new AtomicLong(System.currentTimeMillis() % 100000);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(SubmissionCompletedEvent event) {
        log.info("Handling SubmissionCompletedEvent: submissionId={}", event.getSubmissionId());

        List<SubmissionDetail> details = detailRepository.findBySubmissionId(event.getSubmissionId());
        List<SubmissionDetail> flaggedDetails = details.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsFlagged()))
                .toList();

        if (flaggedDetails.isEmpty()) {
            log.debug("No flagged items found for submission {}", event.getSubmissionId());
            return;
        }

        InspSubmission submission = submissionRepository.findById(event.getSubmissionId())
                .orElse(null);
        if (submission == null) {
            log.warn("Submission {} not found, skipping auto corrective creation", event.getSubmissionId());
            return;
        }

        // 兜底链: event.taskId → submission.taskId → 反查 task.projectId (P0#1)
        Long taskId = event.getTaskId() != null ? event.getTaskId() : submission.getTaskId();
        Long projectId = null;
        if (taskId != null) {
            projectId = taskRepository.findById(taskId)
                    .map(InspTask::getProjectId)
                    .orElse(null);
        }
        if (projectId == null) {
            log.error("Cannot resolve projectId for submission {} (taskId={}). " +
                    "Skipping auto corrective creation to avoid orphan cases.",
                    event.getSubmissionId(), taskId);
            return;
        }

        // 第一次尝试同步执行 (绝大多数情况下成功). 失败的走调度器异步重试.
        for (SubmissionDetail detail : flaggedDetails) {
            tryCreateCase(event, detail, projectId, taskId, submission, 1);
        }
    }

    /**
     * 单次创建尝试 — 成功直接结束; 失败则调度延迟重试 (不 sleep, 不阻塞当前线程).
     */
    private void tryCreateCase(SubmissionCompletedEvent event, SubmissionDetail detail,
                                Long projectId, Long taskId, InspSubmission submission, int attempt) {
        try {
            String caseCode = generateCaseCode();
            String issueDesc = buildIssueDescription(detail);

            caseService.createCase(
                    caseCode,
                    issueDesc,
                    CasePriority.MEDIUM,
                    event.getSubmissionId(),
                    detail.getId(),
                    projectId,
                    taskId,
                    event.getTargetType(),
                    event.getTargetId(),
                    submission.getTargetName(),
                    null,
                    LocalDateTime.now().plusDays(7),
                    null
            );

            if (attempt > 1) {
                log.info("Auto-created corrective case (attempt {}/{}) for detail {}: {}",
                        attempt, MAX_RETRY_ATTEMPTS, detail.getId(), detail.getItemName());
            } else {
                log.info("Auto-created corrective case {} for flagged item: {}",
                        caseCode, detail.getItemName());
            }
        } catch (Exception e) {
            log.warn("Attempt {}/{} failed for detail {}: {}",
                    attempt, MAX_RETRY_ATTEMPTS, detail.getId(), e.getMessage());
            if (attempt < MAX_RETRY_ATTEMPTS) {
                long delaySeconds = 1L << (attempt - 1); // 1s, 2s, 4s
                Instant runAt = Instant.now().plusSeconds(delaySeconds);
                taskScheduler.schedule(
                        () -> tryCreateCase(event, detail, projectId, taskId, submission, attempt + 1),
                        runAt);
            } else {
                handleFinalFailure(event, detail, e);
            }
        }
    }

    private void handleFinalFailure(SubmissionCompletedEvent event, SubmissionDetail detail, Exception lastError) {
        log.error("AUTO_CORRECTIVE_CREATION_FAILED submission={} detail={} item={} attempts={} lastError={}",
                event.getSubmissionId(), detail.getId(), detail.getItemName(),
                MAX_RETRY_ATTEMPTS, lastError != null ? lastError.getMessage() : "unknown",
                lastError);
        try {
            eventPublisher.publish(new AutoCorrectiveCreationFailedEvent(
                    event.getSubmissionId(), detail.getId(), detail.getItemName(),
                    MAX_RETRY_ATTEMPTS,
                    lastError != null ? lastError.getMessage() : "unknown"));
        } catch (Exception ee) {
            log.error("Failed to publish AutoCorrectiveCreationFailedEvent: {}", ee.getMessage());
        }
    }

    private String generateCaseCode() {
        return "CC-" + System.currentTimeMillis() + "-" + CODE_SEQUENCE.incrementAndGet();
    }

    private String buildIssueDescription(SubmissionDetail detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(detail.getSectionName()).append("] ");
        sb.append(detail.getItemName());
        if (detail.getFlagReason() != null && !detail.getFlagReason().isBlank()) {
            sb.append(" — ").append(detail.getFlagReason());
        }
        return sb.toString();
    }
}
