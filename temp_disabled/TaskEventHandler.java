package com.school.management.application.events;

import com.school.management.domain.task.event.*;
import com.school.management.infrastructure.event.DomainEventStore;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.service.OperationLogService;
import com.school.management.entity.OperationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务领域事件处理器
 * 负责处理任务相关的领域事件，包括：
 * - 任务创建
 * - 任务接受
 * - 任务提交
 * - 任务审批通过/拒绝
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventHandler {

    private final DomainEventStore eventStore;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    /**
     * 处理任务创建事件
     */
    @Async
    @EventListener
    public void handle(TaskCreatedEvent event) {
        log.info("Handling TaskCreatedEvent: taskCode={}, title={}, assignerId={}, targetCount={}",
                 event.getTaskCode(), event.getTitle(), event.getAssignerId(),
                 event.getTargetIds() != null ? event.getTargetIds().size() : 0);

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("CREATE", "TASK", parseLongId(event.getAggregateId()),
                String.format("创建任务: %s (%s)", event.getTitle(), event.getTaskCode()));

        // 发送任务分配通知给所有目标用户
        List<Long> targetIds = event.getTargetIds();
        if (targetIds != null && !targetIds.isEmpty()) {
            String message = String.format("%s 给您分配了新任务: [%s]，请及时处理",
                    event.getAssignerName(), event.getTitle());

            notificationService.sendInAppMessageBatch(
                    targetIds,
                    "新任务通知",
                    message,
                    NotificationService.MessageType.TASK_ASSIGNED
            );

            log.info("已发送任务通知给 {} 个用户", targetIds.size());
        }

        log.info("任务创建事件处理完成: taskCode={}", event.getTaskCode());
    }

    /**
     * 处理任务接受事件
     */
    @Async
    @EventListener
    public void handle(TaskAcceptedEvent event) {
        log.info("Handling TaskAcceptedEvent: taskCode={}, assigneeId={}",
                 event.getTaskCode(), event.getAssigneeId());

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("ACCEPT", "TASK", parseLongId(event.getAggregateId()),
                String.format("接受任务: %s", event.getTaskCode()));

        log.info("任务接受事件处理完成: taskCode={}", event.getTaskCode());
    }

    /**
     * 处理任务提交事件
     */
    @Async
    @EventListener
    public void handle(TaskSubmittedEvent event) {
        log.info("Handling TaskSubmittedEvent: taskCode={}, submitterId={}, submitterName={}",
                 event.getTaskCode(), event.getSubmitterId(), event.getSubmitterName());

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("SUBMIT", "TASK", parseLongId(event.getAggregateId()),
                String.format("提交任务: %s (提交人: %s)", event.getTaskCode(), event.getSubmitterName()));

        // 发送通知给审批人（这里暂时使用管理员ID，实际应该从任务流程获取）
        notificationService.sendInAppMessage(
                1L, // 审批人ID
                "任务待审批",
                String.format("%s 提交了任务 [%s]，请及时审批",
                        event.getSubmitterName(), event.getTaskCode()),
                NotificationService.MessageType.TASK_REMINDER
        );

        log.info("任务提交事件处理完成: taskCode={}", event.getTaskCode());
    }

    /**
     * 处理任务审批通过事件
     */
    @Async
    @EventListener
    public void handle(TaskApprovedEvent event) {
        log.info("Handling TaskApprovedEvent: taskCode={}, approverId={}, approverName={}",
                 event.getTaskCode(), event.getApproverId(), event.getApproverName());

        eventStore.store(event);

        // 记录操作日志
        String logDesc = String.format("审批通过任务: %s (审批人: %s, 备注: %s)",
                event.getTaskCode(), event.getApproverName(),
                event.getComment() != null ? event.getComment() : "无");
        saveOperationLog("APPROVE", "TASK", parseLongId(event.getAggregateId()), logDesc);

        // 发送通知给任务提交人（这里暂时使用管理员ID，实际应该从任务获取提交人）
        String message = String.format("您的任务 [%s] 已被 %s 审批通过",
                event.getTaskCode(), event.getApproverName());
        if (event.getComment() != null && !event.getComment().isEmpty()) {
            message += "，审批意见: " + event.getComment();
        }

        notificationService.sendInAppMessage(
                1L, // 任务提交人ID
                "任务审批通过",
                message,
                NotificationService.MessageType.TASK_APPROVED
        );

        log.info("任务审批通过事件处理完成: taskCode={}", event.getTaskCode());
    }

    /**
     * 处理任务审批拒绝事件
     */
    @Async
    @EventListener
    public void handle(TaskRejectedEvent event) {
        log.info("Handling TaskRejectedEvent: taskCode={}, approverId={}, approverName={}, reason={}",
                 event.getTaskCode(), event.getApproverId(), event.getApproverName(), event.getReason());

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("REJECT", "TASK", parseLongId(event.getAggregateId()),
                String.format("审批拒绝任务: %s (审批人: %s, 原因: %s)",
                        event.getTaskCode(), event.getApproverName(),
                        event.getReason() != null ? event.getReason() : "无"));

        // 发送通知给任务提交人
        String message = String.format("您的任务 [%s] 被 %s 退回",
                event.getTaskCode(), event.getApproverName());
        if (event.getReason() != null && !event.getReason().isEmpty()) {
            message += "，退回原因: " + event.getReason();
        }

        notificationService.sendInAppMessage(
                1L, // 任务提交人ID
                "任务被退回",
                message,
                NotificationService.MessageType.TASK_REJECTED
        );

        log.info("任务审批拒绝事件处理完成: taskCode={}", event.getTaskCode());
    }

    /**
     * 解析Long类型ID
     */
    private Long parseLongId(String aggregateId) {
        try {
            return Long.parseLong(aggregateId);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 保存操作日志
     */
    private void saveOperationLog(String operationType, String targetType, Long targetId, String description) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setOperationType(operationType);
            operationLog.setOperationModule(targetType);
            operationLog.setOperationName(description);
            operationLog.setUserId(0L); // 系统操作
            operationLog.setUsername("SYSTEM");
            operationLog.setRealName("系统");
            operationLogService.saveLog(operationLog);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }
}
