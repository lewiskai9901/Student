package com.school.management.application.events;

import com.school.management.domain.student.event.*;
import com.school.management.domain.student.model.valueobject.StudentStatus;
import com.school.management.infrastructure.event.DomainEventStore;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 学生领域事件处理器
 * 负责处理学生相关的领域事件，包括：
 * - 学生入学
 * - 学籍状态变更
 * - 学生信息更新
 * - 班级创建/状态变更
 * - 教师任职分配
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentEventHandler {

    private final DomainEventStore eventStore;
    private final NotificationService notificationService;
    private final ActivityEventPublisher activityEventPublisher;

    // ==================== 学生事件 ====================

    /**
     * 处理学生入学事件
     */
    @Async
    @EventListener
    public void handle(StudentEnrolledEvent event) {
        log.info("Handling StudentEnrolledEvent: studentNo={}, studentName={}, classId={}",
                 event.getStudentNo(), event.getStudentName(), event.getOrgUnitId());

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("CREATE", "STUDENT", parseStudentId(event.getAggregateId()),
                "学生入学: " + event.getStudentName() + " (" + event.getStudentNo() + ")");

        // TODO: resolve class teacher userId from classId via access_relations and send enrollment notification
        if (event.getOrgUnitId() != null) {
            log.debug("Skipping class teacher notification for enrollment (no target user resolution yet): studentName={}, classId={}",
                    event.getStudentName(), event.getOrgUnitId());
        }

        log.info("学生入学事件处理完成: studentNo={}", event.getStudentNo());
    }

    /**
     * 处理学籍状态变更事件
     */
    @Async
    @EventListener
    public void handle(StudentStatusChangedEvent event) {
        log.info("Handling StudentStatusChangedEvent: studentNo={}, {} -> {}",
                 event.getStudentNo(), event.getOldStatus(), event.getNewStatus());

        eventStore.store(event);

        // 根据新状态进行不同处理
        handleStatusChange(event);
    }

    /**
     * 处理学生信息更新事件
     */
    @Async
    @EventListener
    public void handle(StudentUpdatedEvent event) {
        log.info("Handling StudentUpdatedEvent: studentNo={}, studentName={}",
                 event.getStudentNo(), event.getStudentName());

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("UPDATE", "STUDENT", parseStudentId(event.getAggregateId()),
                "更新学生信息: " + event.getStudentName() + " (" + event.getStudentNo() + ")");

        log.info("学生信息更新事件处理完成: studentNo={}", event.getStudentNo());
    }

    // ==================== 班级事件 ====================

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
        saveOperationLog("CREATE", "CLASS", event.getOrgUnitId(),
                "创建班级: " + event.getClassName() + " (" + event.getClassCode() + ")");

        // TODO: resolve department admin userId(s) from org unit and send notification
        if (event.getOrgUnitId() != null) {
            log.debug("Skipping department admin notification for ClassCreated (no target user resolution yet): className={}, orgUnitId={}",
                    event.getClassName(), event.getOrgUnitId());
        }

        log.info("班级创建事件处理完成: classId={}", event.getOrgUnitId());
    }

    /**
     * 处理班级状态变更事件
     */
    @Async
    @EventListener
    public void handle(ClassStatusChangedEvent event) {
        log.info("Handling ClassStatusChangedEvent: classId={}, {} -> {}",
                 event.getOrgUnitId(), event.getOldStatus(), event.getNewStatus());

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
                 event.getOrgUnitId(), event.getTeacherId(), event.getRole());

        eventStore.store(event);

        // 班主任任命通知
        if (event.isHeadTeacherAssignment()) {
            handleHeadTeacherAssignment(event);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 处理学籍状态变更
     */
    private void handleStatusChange(StudentStatusChangedEvent event) {
        StudentStatus newStatus = event.getNewStatus();
        String studentNo = event.getStudentNo();

        String statusDesc = getStatusDescription(newStatus);

        // 记录操作日志
        saveOperationLog("STATUS_CHANGE", "STUDENT", parseStudentId(event.getAggregateId()),
                String.format("学籍状态变更: %s -> %s, 原因: %s",
                        getStatusDescription(event.getOldStatus()),
                        statusDesc,
                        event.getReason() != null ? event.getReason() : "无"));

        // TODO: resolve target userId(s) from student/class context via access_relations for proper notification routing
        switch (newStatus) {
            case GRADUATED:
                log.info("Student {} graduated (notification pending user resolution)", studentNo);
                break;

            case SUSPENDED:
                log.info("Student {} suspended (notification pending user resolution)", studentNo);
                break;

            case WITHDRAWN:
                log.info("Student {} withdrawn (notification pending user resolution)", studentNo);
                break;

            case EXPELLED:
                log.info("Student {} expelled (notification pending user resolution)", studentNo);
                break;

            default:
                log.debug("Student {} status changed to {}", studentNo, newStatus);
        }
    }

    /**
     * 处理班级毕业
     */
    private void handleClassGraduation(ClassStatusChangedEvent event) {
        log.info("Class {} graduated, processing graduation workflow", event.getClassName());

        saveOperationLog("GRADUATE", "CLASS", event.getOrgUnitId(),
                "班级毕业: " + event.getClassName());

        log.debug("Skipping class teacher notification for graduation (no target user resolution yet): className={}", event.getClassName());

        log.info("班级毕业处理完成: classId={}, className={}", event.getOrgUnitId(), event.getClassName());
    }

    /**
     * 处理班级激活
     */
    private void handleClassActivation(ClassStatusChangedEvent event) {
        log.info("Class {} activated, initializing class configuration", event.getClassName());

        saveOperationLog("ACTIVATE", "CLASS", event.getOrgUnitId(),
                "班级激活: " + event.getClassName());

        log.debug("Skipping activation notification (no target user resolution yet): className={}", event.getClassName());

        log.info("班级激活处理完成: classId={}", event.getOrgUnitId());
    }

    /**
     * 处理班主任任命
     */
    private void handleHeadTeacherAssignment(TeacherAssignedEvent event) {
        log.info("Head teacher {} assigned to class {}", event.getTeacherName(), event.getClassName());

        saveOperationLog("ASSIGN", "TEACHER", event.getTeacherId(),
                String.format("任命班主任: %s -> %s", event.getTeacherName(), event.getClassName()));

        // 发送任命通知给班主任
        notificationService.sendInAppMessage(
                event.getTeacherId(),
                "班主任任命通知",
                String.format("您已被任命为班级 [%s] 的班主任，请及时处理班级相关事务", event.getClassName()),
                NotificationService.MessageType.TASK_ASSIGNED
        );

        log.info("班主任任命处理完成: teacherId={}, classId={}", event.getTeacherId(), event.getOrgUnitId());
    }

    /**
     * 获取状态描述
     */
    private String getStatusDescription(StudentStatus status) {
        if (status == null) return "未知";
        return status.getDescription();
    }

    /**
     * 解析学生ID
     */
    private Long parseStudentId(String aggregateId) {
        try {
            return Long.parseLong(aggregateId);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 保存操作日志
     */
    private void saveOperationLog(String action, String resourceType, Long resourceId, String description) {
        try {
            activityEventPublisher.newEvent("student", resourceType, action, description)
                .resourceId(resourceId != null ? resourceId.toString() : "")
                .publish();
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }
}
