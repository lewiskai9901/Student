package com.school.management.application.events;

import com.school.management.domain.asset.event.*;
import com.school.management.infrastructure.event.DomainEventStore;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.infrastructure.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 资产领域事件处理器
 * 负责处理资产相关的领域事件，包括：
 * - 楼宇创建/更新
 * - 宿舍创建
 * - 学生入住/退宿
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetEventHandler {

    private final DomainEventStore eventStore;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    /**
     * 处理楼宇创建事件
     */
    @Async
    @EventListener
    public void handle(BuildingCreatedEvent event) {
        log.info("Handling BuildingCreatedEvent: buildingNo={}, buildingName={}, type={}",
                 event.getBuildingNo(), event.getBuildingName(), event.getBuildingType());

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("CREATE", "BUILDING", parseLongId(event.getAggregateId()),
                "创建楼宇: " + event.getBuildingName() + " (" + event.getBuildingNo() + ")");

        // 发送通知给后勤管理员
        notificationService.sendInAppMessage(
                1L, // 后勤管理员ID
                "楼宇创建通知",
                String.format("新楼宇 [%s] 已创建", event.getBuildingName()),
                NotificationService.MessageType.SYSTEM_NOTICE
        );

        log.info("楼宇创建事件处理完成: buildingNo={}", event.getBuildingNo());
    }

    /**
     * 处理楼宇更新事件
     */
    @Async
    @EventListener
    public void handle(BuildingUpdatedEvent event) {
        log.info("Handling BuildingUpdatedEvent: buildingId={}", event.getAggregateId());

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("UPDATE", "BUILDING", parseLongId(event.getAggregateId()),
                "更新楼宇信息");

        log.info("楼宇更新事件处理完成: buildingId={}", event.getAggregateId());
    }

    /**
     * 处理宿舍创建事件
     */
    @Async
    @EventListener
    public void handle(DormitoryCreatedEvent event) {
        log.info("Handling DormitoryCreatedEvent: dormitoryNo={}, buildingId={}, bedCapacity={}",
                 event.getDormitoryNo(), event.getBuildingId(), event.getBedCapacity());

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("CREATE", "DORMITORY", parseLongId(event.getAggregateId()),
                String.format("创建宿舍: %s (床位数: %d)", event.getDormitoryNo(), event.getBedCapacity()));

        log.info("宿舍创建事件处理完成: dormitoryNo={}", event.getDormitoryNo());
    }

    /**
     * 处理学生入住事件
     */
    @Async
    @EventListener
    public void handle(StudentCheckedInEvent event) {
        log.info("Handling StudentCheckedInEvent: studentId={}, dormitoryNo={}, bedNumber={}",
                 event.getStudentId(), event.getDormitoryNo(), event.getBedNumber());

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("CHECK_IN", "DORMITORY", parseLongId(event.getAggregateId()),
                String.format("学生入住: %s -> %s-%d床",
                        event.getStudentName(), event.getDormitoryNo(), event.getBedNumber()));

        // 发送入住通知给学生
        notificationService.sendInAppMessage(
                event.getStudentId(),
                "入住确认通知",
                String.format("您已成功入住宿舍 %s 第%d床位，祝您生活愉快！",
                        event.getDormitoryNo(), event.getBedNumber()),
                NotificationService.MessageType.SYSTEM_NOTICE
        );

        // 发送通知给宿舍管理员
        notificationService.sendInAppMessage(
                1L, // 宿舍管理员ID
                "学生入住通知",
                String.format("学生 [%s] 已入住 %s 第%d床位",
                        event.getStudentName(), event.getDormitoryNo(), event.getBedNumber()),
                NotificationService.MessageType.SYSTEM_NOTICE
        );

        log.info("学生入住事件处理完成: studentId={}, dormitoryNo={}", event.getStudentId(), event.getDormitoryNo());
    }

    /**
     * 处理学生退宿事件
     */
    @Async
    @EventListener
    public void handle(StudentCheckedOutEvent event) {
        log.info("Handling StudentCheckedOutEvent: studentId={}, dormitoryNo={}, bedNumber={}",
                 event.getStudentId(), event.getDormitoryNo(), event.getBedNumber());

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("CHECK_OUT", "DORMITORY", parseLongId(event.getAggregateId()),
                String.format("学生退宿: %s <- %s-%d床",
                        event.getStudentName(), event.getDormitoryNo(), event.getBedNumber()));

        // 发送退宿通知给学生
        notificationService.sendInAppMessage(
                event.getStudentId(),
                "退宿确认通知",
                String.format("您已成功从宿舍 %s 第%d床位退宿，请妥善保管个人物品",
                        event.getDormitoryNo(), event.getBedNumber()),
                NotificationService.MessageType.SYSTEM_NOTICE
        );

        // 发送通知给宿舍管理员
        notificationService.sendInAppMessage(
                1L, // 宿舍管理员ID
                "学生退宿通知",
                String.format("学生 [%s] 已从 %s 第%d床位退宿，床位已释放",
                        event.getStudentName(), event.getDormitoryNo(), event.getBedNumber()),
                NotificationService.MessageType.SYSTEM_NOTICE
        );

        log.info("学生退宿事件处理完成: studentId={}, dormitoryNo={}", event.getStudentId(), event.getDormitoryNo());
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
            auditLogService.logCreate(targetType, targetId != null ? String.valueOf(targetId) : "", description, null, description);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }
}
