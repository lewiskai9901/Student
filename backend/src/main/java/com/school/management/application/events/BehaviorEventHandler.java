package com.school.management.application.events;

import com.school.management.application.behavior.BehaviorApplicationService;
import com.school.management.domain.behavior.event.*;
import com.school.management.infrastructure.event.DomainEventStore;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.infrastructure.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BehaviorEventHandler {

    private final DomainEventStore eventStore;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final BehaviorApplicationService behaviorService;

    @Async
    @EventListener
    public void handle(BehaviorRecordedEvent event) {
        log.info("Handling BehaviorRecordedEvent: recordId={}, studentId={}, type={}",
                event.getBehaviorRecordId(), event.getStudentId(), event.getBehaviorType());

        eventStore.store(event);

        saveOperationLog("CREATE", "BEHAVIOR_RECORD", event.getBehaviorRecordId(),
                "记录学生行为: " + event.getTitle());

        // Check and generate alerts for violations
        if (event.isViolation()) {
            try {
                behaviorService.checkAndGenerateAlerts(event.getStudentId(), event.getClassId());
            } catch (Exception e) {
                log.warn("生成行为预警失败: {}", e.getMessage());
            }
        }
    }

    @Async
    @EventListener
    public void handle(BehaviorStatusChangedEvent event) {
        log.info("Handling BehaviorStatusChangedEvent: recordId={}, {} -> {}",
                event.getBehaviorRecordId(), event.getPreviousStatus(), event.getNewStatus());
        eventStore.store(event);
    }

    @Async
    @EventListener
    public void handle(AlertTriggeredEvent event) {
        log.info("Handling AlertTriggeredEvent: alertId={}, studentId={}, type={}",
                event.getAlertId(), event.getStudentId(), event.getAlertType());

        eventStore.store(event);

        saveOperationLog("ALERT", "BEHAVIOR_ALERT", event.getAlertId(),
                "触发行为预警: " + event.getTitle());

        // Notify class teacher (using classId, in production would look up actual teacher)
        notificationService.sendInAppMessage(
                1L, // Would be resolved to actual class teacher in production
                "学生行为预警",
                String.format("[%s] %s", event.getAlertLevel(), event.getTitle()),
                NotificationService.MessageType.BEHAVIOR_ALERT
        );
    }

    private void saveOperationLog(String operationType, String targetType, Long targetId, String description) {
        try {
            auditLogService.logCreate(targetType, targetId != null ? String.valueOf(targetId) : "", description, null, description);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }
}
