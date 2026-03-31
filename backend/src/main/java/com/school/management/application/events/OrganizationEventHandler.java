package com.school.management.application.events;

import com.school.management.domain.organization.event.*;
import com.school.management.infrastructure.event.DomainEventStore;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


/**
 * 组织管理领域事件处理器
 * 负责处理组织架构相关的领域事件，包括：
 * - 组织单元创建/更新
 * - 班级创建/状态变更
 * - 教师任职分配
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationEventHandler {

    private final DomainEventStore eventStore;
    private final NotificationService notificationService;
    private final ActivityEventPublisher activityEventPublisher;

    /**
     * 处理组织单元创建事件
     */
    @Async
    @EventListener
    public void handle(OrgUnitCreatedEvent event) {
        log.info("Handling OrgUnitCreatedEvent: unitCode={}, unitName={}",
                 event.getUnitCode(), event.getUnitName());

        // 存储事件
        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("CREATE", "ORG_UNIT", event.getOrgUnitId(),
                "创建组织单元: " + event.getUnitName() + " (" + event.getUnitCode() + ")");

        // TODO: resolve actual admin userId(s) via role-based lookup and send notification
        log.debug("Skipping admin notification for OrgUnitCreated (no target user resolution yet): unitName={}", event.getUnitName());
    }

    /**
     * 处理组织单元更新事件
     */
    @Async
    @EventListener
    public void handle(OrgUnitUpdatedEvent event) {
        log.info("Handling OrgUnitUpdatedEvent: unitId={}", event.getOrgUnitId());

        eventStore.store(event);
    }

    /**
     * 处理班级创建事件
     */
    @Async
    @EventListener
    public void handle(ClassCreatedEvent event) {
        log.info("Handling ClassCreatedEvent: classCode={}, className={}",
                 event.getClassCode(), event.getClassName());

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("CREATE", "CLASS", event.getClassId(),
                "创建班级: " + event.getClassName() + " (" + event.getClassCode() + ")");

        // TODO: resolve department admin userId(s) from org unit and send notification
        if (event.getOrgUnitId() != null) {
            log.debug("Skipping department admin notification for ClassCreated (no target user resolution yet): className={}, orgUnitId={}",
                    event.getClassName(), event.getOrgUnitId());
        }

        log.info("班级创建事件处理完成: classId={}", event.getClassId());
    }

    /**
     * 处理班级状态变更事件
     */
    @Async
    @EventListener
    public void handle(ClassStatusChangedEvent event) {
        log.info("Handling ClassStatusChangedEvent: classId={}, {} -> {}",
                 event.getClassId(), event.getOldStatus(), event.getNewStatus());

        eventStore.store(event);

        // 处理毕业事件
        if (event.isGraduation()) {
            handleClassGraduation(event);
        }

        // 处理激活事件
        if (event.isActivation()) {
            handleClassActivation(event);
        }
    }

    /**
     * 处理教师任职事件
     */
    @Async
    @EventListener
    public void handle(TeacherAssignedEvent event) {
        log.info("Handling TeacherAssignedEvent: classId={}, teacherId={}, role={}",
                 event.getClassId(), event.getTeacherId(), event.getRole());

        eventStore.store(event);

        // 班主任任命通知
        if (event.isHeadTeacherAssignment()) {
            handleHeadTeacherAssignment(event);
        }
    }

    /**
     * 处理班级毕业
     * - 更新学生状态为毕业
     * - 生成毕业统计报表
     * - 归档班级数据
     */
    private void handleClassGraduation(ClassStatusChangedEvent event) {
        log.info("Class {} graduated, processing graduation workflow", event.getClassName());

        // 记录操作日志
        saveOperationLog("GRADUATE", "CLASS", event.getClassId(),
                "班级毕业: " + event.getClassName());

        // TODO: resolve class teacher userId from class context and send graduation notification
        log.debug("Skipping class teacher notification for graduation (no target user resolution yet): className={}", event.getClassName());

        log.info("班级毕业处理完成: classId={}, className={}", event.getClassId(), event.getClassName());
    }

    /**
     * 处理班级激活
     * - 发送开班通知
     * - 初始化量化检查配置
     */
    private void handleClassActivation(ClassStatusChangedEvent event) {
        log.info("Class {} activated, initializing class configuration", event.getClassName());

        // 记录操作日志
        saveOperationLog("ACTIVATE", "CLASS", event.getClassId(),
                "班级激活: " + event.getClassName());

        // TODO: resolve department admin userId(s) from context and send activation notification
        log.debug("Skipping activation notification (no target user resolution yet): className={}", event.getClassName());

        log.info("班级激活处理完成: classId={}", event.getClassId());
    }

    /**
     * 处理班主任任命
     * - 发送任命通知给班主任
     * - 更新权限配置
     */
    private void handleHeadTeacherAssignment(TeacherAssignedEvent event) {
        log.info("Head teacher {} assigned to class {}", event.getTeacherName(), event.getClassName());

        // 记录操作日志
        saveOperationLog("ASSIGN", "TEACHER", event.getTeacherId(),
                String.format("任命班主任: %s -> %s", event.getTeacherName(), event.getClassName()));

        // 发送任命通知给班主任
        notificationService.sendInAppMessage(
                event.getTeacherId(),
                "班主任任命通知",
                String.format("您已被任命为班级 [%s] 的班主任，请及时处理班级相关事务", event.getClassName()),
                NotificationService.MessageType.TASK_ASSIGNED
        );

        log.info("班主任任命处理完成: teacherId={}, classId={}", event.getTeacherId(), event.getClassId());
    }

    /**
     * 保存操作日志
     */
    private void saveOperationLog(String action, String resourceType, Long resourceId, String description) {
        try {
            activityEventPublisher.newEvent("organization", resourceType, action, description)
                .resourceId(resourceId != null ? resourceId.toString() : "")
                .publish();
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }
}
