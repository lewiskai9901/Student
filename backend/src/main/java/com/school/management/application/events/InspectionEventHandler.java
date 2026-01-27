package com.school.management.application.events;

import com.school.management.domain.inspection.event.*;
import com.school.management.domain.inspection.model.AppealStatus;
import com.school.management.infrastructure.event.DomainEventStore;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.infrastructure.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 量化检查领域事件处理器
 * 负责处理量化检查相关的领域事件，包括：
 * - 检查模板创建
 * - 检查记录发布
 * - 申诉状态变更
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionEventHandler {

    private final DomainEventStore eventStore;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    /**
     * 处理检查模板创建事件
     */
    @Async
    @EventListener
    public void handle(InspectionTemplateCreatedEvent event) {
        log.info("Handling InspectionTemplateCreatedEvent: templateId={}, templateName={}",
                 event.getTemplateId(), event.getTemplateName());

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("CREATE", "INSPECTION_TEMPLATE", event.getTemplateId(),
                "创建检查模板: " + event.getTemplateName());

        // 发送通知给管理员
        notificationService.sendInAppMessage(
                1L, // 管理员ID
                "检查模板创建",
                String.format("新检查模板 [%s] 已创建", event.getTemplateName()),
                NotificationService.MessageType.SYSTEM_NOTICE
        );

        log.info("检查模板创建事件处理完成: templateId={}", event.getTemplateId());
    }

    /**
     * 处理检查记录发布事件
     */
    @Async
    @EventListener
    public void handle(InspectionRecordPublishedEvent event) {
        log.info("Handling InspectionRecordPublishedEvent: recordId={}",
                 event.getRecordId());

        eventStore.store(event);

        // 发布后的处理
        handleRecordPublished(event);
    }

    /**
     * 处理申诉状态变更事件
     */
    @Async
    @EventListener
    public void handle(AppealStatusChangedEvent event) {
        log.info("Handling AppealStatusChangedEvent: appealId={}, {} -> {}",
                 event.getAppealId(), event.getPreviousStatus(), event.getNewStatus());

        eventStore.store(event);

        // 根据新状态进行不同处理
        handleAppealStatusChange(event);
    }

    /**
     * 处理检查记录发布
     * - 计算班级得分排名
     * - 更新班级量化统计
     * - 发送通知给相关班主任
     * - 触发评级计算
     */
    private void handleRecordPublished(InspectionRecordPublishedEvent event) {
        log.info("Inspection record {} published, processing notification and statistics", event.getRecordId());

        // 记录操作日志
        saveOperationLog("PUBLISH", "INSPECTION_RECORD", event.getRecordId(),
                "发布检查记录");

        // 发送通知给相关班主任（实际应该从检查记录中获取涉及的班级班主任列表）
        notificationService.sendInAppMessage(
                1L, // 班主任ID
                "量化检查结果发布",
                "新的量化检查结果已发布，请及时查看",
                NotificationService.MessageType.INSPECTION_PUBLISHED
        );

        log.info("检查记录发布处理完成: recordId={}", event.getRecordId());
    }

    /**
     * 处理申诉状态变更
     * 根据不同的状态发送相应的通知
     */
    private void handleAppealStatusChange(AppealStatusChangedEvent event) {
        AppealStatus newStatus = event.getNewStatus();
        Long appealId = event.getAppealId();

        switch (newStatus) {
            case PENDING:
                log.info("Appeal {} submitted, notifying reviewers", appealId);
                // 发送通知给初审人员
                notificationService.sendInAppMessage(
                        1L, // 初审人员ID（实际应该从申诉记录获取）
                        "新申诉待审核",
                        String.format("收到新的申诉请求(ID:%d)，请及时处理", appealId),
                        NotificationService.MessageType.APPEAL_SUBMITTED
                );
                saveOperationLog("SUBMIT", "APPEAL", appealId, "提交申诉");
                break;

            case LEVEL1_APPROVED:
                log.info("Appeal {} first approved, notifying final reviewers", appealId);
                // 发送通知给终审人员
                notificationService.sendInAppMessage(
                        1L, // 终审人员ID
                        "申诉待终审",
                        String.format("申诉(ID:%d)已通过初审，请进行终审", appealId),
                        NotificationService.MessageType.APPEAL_SUBMITTED
                );
                saveOperationLog("LEVEL1_APPROVE", "APPEAL", appealId, "初审通过");
                break;

            case APPROVED:
                log.info("Appeal {} final approved, processing score update", appealId);
                // 发送通知给申诉人（由于事件中未包含申诉人ID，这里使用changedBy或默认值）
                notificationService.sendInAppMessage(
                        event.getChangedBy() != null ? event.getChangedBy() : 1L,
                        "申诉通过通知",
                        String.format("您的申诉(ID:%d)已通过审批，相关分数将被调整", appealId),
                        NotificationService.MessageType.APPEAL_RESULT
                );
                saveOperationLog("APPROVE", "APPEAL", appealId, "终审通过");
                break;

            case REJECTED:
            case LEVEL1_REJECTED:
                log.info("Appeal {} rejected, notifying appealer", appealId);
                // 发送拒绝通知给申诉人（由于事件中未包含申诉人ID，这里使用changedBy或默认值）
                notificationService.sendInAppMessage(
                        event.getChangedBy() != null ? event.getChangedBy() : 1L,
                        "申诉驳回通知",
                        String.format("您的申诉(ID:%d)未通过审批，请查看详细原因", appealId),
                        NotificationService.MessageType.APPEAL_RESULT
                );
                saveOperationLog("REJECT", "APPEAL", appealId, "申诉被驳回");
                break;

            default:
                log.debug("Appeal {} status changed to {}", appealId, newStatus);
        }
    }

    /**
     * 保存操作日志
     */
    private void saveOperationLog(String operationType, String targetType, Long targetId, String description) {
        try {
            auditLogService.logCreate(targetType, targetId != null ? String.valueOf(targetId) : "", description, null, description);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }
}
