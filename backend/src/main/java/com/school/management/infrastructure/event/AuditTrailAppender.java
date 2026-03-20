package com.school.management.infrastructure.event;

import com.school.management.application.inspection.v7.AuditTrailApplicationService;
import com.school.management.domain.inspection.event.v7.InspV7DomainEvent;
import com.school.management.domain.shared.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 审计日志追加器 — 监听所有 InspV7DomainEvent，自动记录审计日志。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AuditTrailAppender {

    private final AuditTrailApplicationService auditTrailService;

    @Async
    @EventListener
    public void onInspV7Event(InspV7DomainEvent event) {
        try {
            DomainEvent domainEvent = (DomainEvent) event;
            String eventType = event.getClass().getSimpleName();
            String action = mapEventToAction(eventType);
            String resourceType = domainEvent.getAggregateType();
            String resourceIdStr = domainEvent.getAggregateId();
            Long resourceId = null;
            if (resourceIdStr != null) {
                try {
                    resourceId = Long.parseLong(resourceIdStr);
                } catch (NumberFormatException ignored) {}
            }

            // Get current user from SecurityContext (may be null in async context)
            Long userId = null;
            String userName = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                try {
                    var userDetails = (com.school.management.security.CustomUserDetails) auth.getPrincipal();
                    userId = userDetails.getUserId();
                    userName = userDetails.getUsername();
                } catch (ClassCastException e) {
                    userName = auth.getName();
                }
            }
            if (userId == null) userId = 0L;
            if (userName == null) userName = "system";

            // AuditTrailApplicationService.record signature:
            // record(Long tenantId, Long userId, String userName, String action,
            //        String resourceType, Long resourceId, String resourceName,
            //        String details, String ipAddress)
            auditTrailService.record(null, userId, userName, action, resourceType,
                    resourceId, null, null, null);

            log.debug("AuditTrail appended: action={}, resource={}:{}", action, resourceType, resourceId);
        } catch (Exception e) {
            log.error("AuditTrailAppender failed for event {}: {}", event.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    private String mapEventToAction(String eventType) {
        // Map event class names to human-readable action strings
        return switch (eventType) {
            case "TemplatePublishedEvent" -> "PUBLISH_TEMPLATE";
            case "ProjectPublishedEvent" -> "PUBLISH_PROJECT";
            case "ProjectPausedEvent" -> "PAUSE_PROJECT";
            case "ProjectResumedEvent" -> "RESUME_PROJECT";
            case "ProjectCompletedEvent" -> "COMPLETE_PROJECT";
            case "TaskCreatedEvent" -> "CREATE_TASK";
            case "TaskPublishedEvent" -> "PUBLISH_TASK";
            case "TaskClaimedEvent" -> "CLAIM_TASK";
            case "TaskStartedEvent" -> "START_TASK";
            case "TaskSubmittedEvent" -> "SUBMIT_TASK";
            case "TaskReviewedEvent" -> "REVIEW_TASK";
            case "TaskCancelledEvent" -> "CANCEL_TASK";
            case "TaskExpiredEvent" -> "EXPIRE_TASK";
            case "SubmissionCompletedEvent" -> "COMPLETE_SUBMISSION";
            case "CorrectiveCaseCreatedEvent" -> "CREATE_CORRECTIVE_CASE";
            case "CaseAssignedEvent" -> "ASSIGN_CASE";
            case "CorrectionSubmittedEvent" -> "SUBMIT_CORRECTION";
            case "CaseVerifiedEvent" -> "VERIFY_CASE";
            case "CaseRejectedEvent" -> "REJECT_CASE";
            case "CaseEscalatedEvent" -> "ESCALATE_CASE";
            case "CaseClosedEvent" -> "CLOSE_CASE";
            case "EffectivenessConfirmedEvent" -> "CONFIRM_EFFECTIVENESS";
            case "EffectivenessFailedEvent" -> "FAIL_EFFECTIVENESS";
            case "SlaBreachedEvent" -> "SLA_BREACH";
            case "DailySummaryUpdatedEvent" -> "UPDATE_DAILY_SUMMARY";
            case "PeriodSummaryCalculatedEvent" -> "CALCULATE_PERIOD_SUMMARY";
            default -> eventType.replace("Event", "").toUpperCase();
        };
    }
}
