package com.school.management.application.events;

import com.school.management.application.corrective.CorrectiveActionApplicationService;
import com.school.management.domain.corrective.event.*;
import com.school.management.domain.corrective.model.AutoActionRule;
import com.school.management.domain.corrective.repository.AutoActionRuleRepository;
import com.school.management.domain.inspection.event.SessionPublishedEvent;
import com.school.management.infrastructure.event.DomainEventStore;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.infrastructure.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorrectiveActionEventHandler {

    private final DomainEventStore eventStore;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final AutoActionRuleRepository ruleRepository;

    @Async
    @EventListener
    public void handle(ActionCreatedEvent event) {
        log.info("Handling ActionCreatedEvent: actionId={}, actionCode={}",
                event.getActionId(), event.getActionCode());

        eventStore.store(event);

        saveOperationLog("CREATE", "CORRECTIVE_ACTION", event.getActionId(),
                "创建整改工单: " + event.getTitle());

        if (event.getAssigneeId() != null) {
            notificationService.sendInAppMessage(
                    event.getAssigneeId(),
                    "整改工单分配",
                    String.format("您有新的整改工单 [%s] 需要处理", event.getTitle()),
                    NotificationService.MessageType.CORRECTIVE_ACTION_ASSIGNED
            );
        }
    }

    @Async
    @EventListener
    public void handle(ActionStatusChangedEvent event) {
        log.info("Handling ActionStatusChangedEvent: actionId={}, {} -> {}",
                event.getActionId(), event.getPreviousStatus(), event.getNewStatus());
        eventStore.store(event);
    }

    @Async
    @EventListener
    public void handle(ActionOverdueEvent event) {
        log.info("Handling ActionOverdueEvent: actionId={}", event.getActionId());

        eventStore.store(event);

        saveOperationLog("OVERDUE", "CORRECTIVE_ACTION", event.getActionId(),
                "整改工单超期: " + event.getActionCode());

        // Notify admin about overdue
        notificationService.sendInAppMessage(
                1L,
                "整改工单超期",
                String.format("整改工单 [%s] 已超期，严重程度: %s", event.getActionCode(), event.getSeverity()),
                NotificationService.MessageType.CORRECTIVE_ACTION_OVERDUE
        );

        if (event.getAssigneeId() != null) {
            notificationService.sendInAppMessage(
                    event.getAssigneeId(),
                    "整改工单超期提醒",
                    String.format("您的整改工单 [%s] 已超期，请尽快处理", event.getActionCode()),
                    NotificationService.MessageType.CORRECTIVE_ACTION_OVERDUE
            );
        }
    }

    @Async
    @EventListener
    public void handle(ActionVerifiedEvent event) {
        log.info("Handling ActionVerifiedEvent: actionId={}, result={}",
                event.getActionId(), event.getResult());

        eventStore.store(event);

        saveOperationLog("VERIFY", "CORRECTIVE_ACTION", event.getActionId(),
                "验证整改: " + event.getResult());

        if (event.getVerifierId() != null) {
            String title = event.isPassed() ? "整改验证通过" : "整改验证未通过";
            String content = event.isPassed()
                    ? String.format("整改工单 [%s] 已通过验证", event.getActionCode())
                    : String.format("整改工单 [%s] 未通过验证，需重新整改", event.getActionCode());

            notificationService.sendInAppMessage(
                    event.getVerifierId(),
                    title,
                    content,
                    NotificationService.MessageType.CORRECTIVE_ACTION_VERIFIED
            );
        }
    }

    @Async
    @EventListener
    public void handle(ActionEscalatedEvent event) {
        log.info("Handling ActionEscalatedEvent: actionId={}, level={}",
                event.getActionId(), event.getEscalationLevel());
        eventStore.store(event);

        notificationService.sendInAppMessage(
                1L,
                "整改工单已升级",
                String.format("整改工单 [%s] 已升级至第 %d 级", event.getActionCode(), event.getEscalationLevel()),
                NotificationService.MessageType.CORRECTIVE_ACTION_OVERDUE
        );
    }

    @Async
    @EventListener
    public void handleSessionPublished(SessionPublishedEvent event) {
        log.info("CorrectiveActionEventHandler: evaluating auto-action rules for session {}",
                event.getSessionId());

        List<AutoActionRule> rules = ruleRepository.findByTriggerType("INSPECTION_PUBLISHED");
        for (AutoActionRule rule : rules) {
            if (rule.evaluate("INSPECTION_PUBLISHED", null)) {
                log.info("Auto-action rule {} triggered for session {}", rule.getRuleCode(), event.getSessionId());
                // Auto-creation would be handled here with full session data
            }
        }
    }

    private void saveOperationLog(String operationType, String targetType, Long targetId, String description) {
        try {
            auditLogService.logCreate(targetType, targetId != null ? String.valueOf(targetId) : "", description, null, description);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }
}
