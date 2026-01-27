package com.school.management.application.events;

import com.school.management.domain.schedule.event.*;
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
public class ScheduleEventHandler {

    private final DomainEventStore eventStore;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Async
    @EventListener
    public void handle(PolicyCreatedEvent event) {
        log.info("Handling PolicyCreatedEvent: policyId={}, policyCode={}",
                event.getPolicyId(), event.getPolicyCode());
        eventStore.store(event);
        saveOperationLog("CREATE", "SCHEDULE_POLICY", event.getPolicyId(),
                "创建排班策略: " + event.getPolicyName());
    }

    @Async
    @EventListener
    public void handle(PolicyStatusChangedEvent event) {
        log.info("Handling PolicyStatusChangedEvent: policyId={}, enabled={}",
                event.getPolicyId(), event.isEnabled());
        eventStore.store(event);
        saveOperationLog("UPDATE", "SCHEDULE_POLICY", event.getPolicyId(),
                event.isEnabled() ? "启用排班策略" : "禁用排班策略");
    }

    @Async
    @EventListener
    public void handle(ExecutionCompletedEvent event) {
        log.info("Handling ExecutionCompletedEvent: executionId={}, policyId={}, date={}",
                event.getExecutionId(), event.getPolicyId(), event.getExecutionDate());
        eventStore.store(event);

        notificationService.sendInAppMessage(
                1L,
                "排班执行完成",
                String.format("排班执行已完成 (日期: %s, 检查员: %s)",
                    event.getExecutionDate(), event.getAssignedInspectors()),
                NotificationService.MessageType.SCHEDULE_EXECUTION_COMPLETED
        );

        saveOperationLog("EXECUTE", "SCHEDULE_EXECUTION", event.getExecutionId(),
                "排班执行完成: " + event.getExecutionDate());
    }

    @Async
    @EventListener
    public void handle(ExecutionFailedEvent event) {
        log.info("Handling ExecutionFailedEvent: executionId={}, reason={}",
                event.getExecutionId(), event.getFailureReason());
        eventStore.store(event);

        notificationService.sendInAppMessage(
                1L,
                "排班执行失败",
                String.format("排班执行失败 (日期: %s, 原因: %s)",
                    event.getExecutionDate(), event.getFailureReason()),
                NotificationService.MessageType.SCHEDULE_EXECUTION_FAILED
        );

        saveOperationLog("FAIL", "SCHEDULE_EXECUTION", event.getExecutionId(),
                "排班执行失败: " + event.getFailureReason());
    }

    private void saveOperationLog(String operationType, String targetType, Long targetId, String description) {
        try {
            auditLogService.logCreate(targetType, targetId != null ? String.valueOf(targetId) : "", description, null, description);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }
}
