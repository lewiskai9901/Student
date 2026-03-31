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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentEventHandler {

    private final DomainEventStore eventStore;
    private final NotificationService notificationService;
    private final ActivityEventPublisher activityEventPublisher;

    /**
     * 处理学生入学事件
     */
    @Async
    @EventListener
    public void handle(StudentEnrolledEvent event) {
        log.info("Handling StudentEnrolledEvent: studentNo={}, studentName={}, classId={}",
                 event.getStudentNo(), event.getStudentName(), event.getClassId());

        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("CREATE", "STUDENT", parseStudentId(event.getAggregateId()),
                "学生入学: " + event.getStudentName() + " (" + event.getStudentNo() + ")");

        // TODO: resolve class teacher userId from classId via access_relations and send enrollment notification
        if (event.getClassId() != null) {
            log.debug("Skipping class teacher notification for enrollment (no target user resolution yet): studentName={}, classId={}",
                    event.getStudentName(), event.getClassId());
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
        // For GRADUATED: should notify the student's own user account
        // For SUSPENDED/WITHDRAWN/EXPELLED: should notify the class teacher
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
