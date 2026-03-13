package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.event.v7.SubmissionCompletedEvent;
import com.school.management.domain.inspection.model.v7.corrective.CasePriority;
import com.school.management.domain.inspection.model.v7.execution.InspSubmission;
import com.school.management.domain.inspection.model.v7.execution.SubmissionDetail;
import com.school.management.domain.inspection.repository.v7.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.v7.SubmissionDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自动整改创建处理器
 * 监听 SubmissionCompletedEvent，为标记为问题的明细项自动创建整改案例
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorrectiveAutoCreationHandler {

    private final SubmissionDetailRepository detailRepository;
    private final InspSubmissionRepository submissionRepository;
    private final CorrectiveCaseApplicationService caseService;

    private static final AtomicLong CODE_SEQUENCE = new AtomicLong(System.currentTimeMillis() % 100000);

    @Async
    @EventListener
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

        // Get submission for context
        InspSubmission submission = submissionRepository.findById(event.getSubmissionId())
                .orElse(null);
        if (submission == null) {
            log.warn("Submission {} not found, skipping auto corrective creation", event.getSubmissionId());
            return;
        }

        for (SubmissionDetail detail : flaggedDetails) {
            try {
                String caseCode = generateCaseCode();
                String issueDesc = buildIssueDescription(detail);

                caseService.createCase(
                        caseCode,
                        issueDesc,
                        CasePriority.MEDIUM,
                        event.getSubmissionId(),
                        detail.getId(),
                        null, // projectId will be resolved from task
                        event.getTaskId(),
                        event.getTargetType(),
                        event.getTargetId(),
                        submission.getTargetName(),
                        null, // requiredAction
                        LocalDateTime.now().plusDays(7), // default 7-day deadline
                        null  // createdBy - system auto-created
                );

                log.info("Auto-created corrective case {} for flagged item: {}",
                        caseCode, detail.getItemName());
            } catch (Exception e) {
                log.error("Failed to auto-create corrective case for detail {}: {}",
                        detail.getId(), e.getMessage(), e);
            }
        }

        log.info("Auto-created {} corrective cases for submission {}",
                flaggedDetails.size(), event.getSubmissionId());
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
