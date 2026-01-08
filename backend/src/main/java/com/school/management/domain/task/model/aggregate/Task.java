package com.school.management.domain.task.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.task.event.*;
import com.school.management.domain.task.model.entity.TaskSubmission;
import com.school.management.domain.task.model.valueobject.TaskPriority;
import com.school.management.domain.task.model.valueobject.TaskStatus;
import com.school.management.exception.BusinessException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务聚合根
 */
@Getter
@Setter
public class Task extends AggregateRoot<Long> {

    private String taskCode;
    private String title;
    private String description;
    private TaskPriority priority;
    private Long assignerId;
    private String assignerName;
    private Integer assignType;
    private Long departmentId;
    private LocalDateTime dueDate;
    private TaskStatus status;
    private List<Long> targetIds;
    private LocalDateTime acceptedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private Long workflowTemplateId;
    private String processInstanceId;
    private String currentNode;
    private List<Long> currentApprovers;
    private List<Long> attachmentIds;
    // version 继承自 AggregateRoot<Long>
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 子实体
    private List<TaskSubmission> submissions = new ArrayList<>();

    protected Task() {}

    /**
     * 创建任务
     */
    public static Task create(String taskCode, String title, String description,
                               TaskPriority priority, Long assignerId, String assignerName,
                               Long departmentId, LocalDateTime dueDate,
                               List<Long> targetIds, Integer assignType) {
        Task task = new Task();
        task.taskCode = taskCode;
        task.title = title;
        task.description = description;
        task.priority = priority;
        task.assignerId = assignerId;
        task.assignerName = assignerName;
        task.departmentId = departmentId;
        task.dueDate = dueDate;
        task.targetIds = targetIds;
        task.assignType = assignType;
        task.status = TaskStatus.PENDING;
        task.setVersion(0L);
        task.createdAt = LocalDateTime.now();
        task.updatedAt = LocalDateTime.now();

        task.registerEvent(new TaskCreatedEvent(
                null,
                taskCode,
                title,
                assignerId,
                assignerName,
                targetIds
        ));

        return task;
    }

    /**
     * 从持久化重建
     */
    public static Task reconstruct(Long id, String taskCode, String title, String description,
                                    TaskPriority priority, Long assignerId, String assignerName,
                                    Integer assignType, Long departmentId, LocalDateTime dueDate,
                                    TaskStatus status, List<Long> targetIds,
                                    LocalDateTime acceptedAt, LocalDateTime submittedAt,
                                    LocalDateTime completedAt, Long workflowTemplateId,
                                    String processInstanceId, String currentNode,
                                    List<Long> currentApprovers, List<Long> attachmentIds,
                                    Integer version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Task task = new Task();
        task.setId(id);
        task.taskCode = taskCode;
        task.title = title;
        task.description = description;
        task.priority = priority;
        task.assignerId = assignerId;
        task.assignerName = assignerName;
        task.assignType = assignType;
        task.departmentId = departmentId;
        task.dueDate = dueDate;
        task.status = status;
        task.targetIds = targetIds;
        task.acceptedAt = acceptedAt;
        task.submittedAt = submittedAt;
        task.completedAt = completedAt;
        task.workflowTemplateId = workflowTemplateId;
        task.processInstanceId = processInstanceId;
        task.currentNode = currentNode;
        task.currentApprovers = currentApprovers;
        task.attachmentIds = attachmentIds;
        task.setVersion(version != null ? version.longValue() : null);
        task.createdAt = createdAt;
        task.updatedAt = updatedAt;
        return task;
    }

    /**
     * 接受任务
     */
    public void accept(Long assigneeId) {
        if (!status.canAccept()) {
            throw new BusinessException("任务状态不允许接受: " + status.getName());
        }

        this.status = TaskStatus.IN_PROGRESS;
        this.acceptedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        registerEvent(new TaskAcceptedEvent(
                String.valueOf(getId()),
                taskCode,
                assigneeId
        ));
    }

    /**
     * 提交任务
     */
    public TaskSubmission submit(Long submitterId, String submitterName, String content, List<Long> attachments) {
        if (!status.canSubmit()) {
            throw new BusinessException("任务状态不允许提交: " + status.getName());
        }

        TaskSubmission submission = TaskSubmission.create(
                getId(),
                submitterId,
                submitterName,
                content,
                attachments
        );
        submissions.add(submission);

        this.status = TaskStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        registerEvent(new TaskSubmittedEvent(
                String.valueOf(getId()),
                taskCode,
                submitterId,
                submitterName
        ));

        return submission;
    }

    /**
     * 审批通过
     */
    public void approve(Long approverId, String approverName, String comment) {
        if (!status.canApprove()) {
            throw new BusinessException("任务状态不允许审批: " + status.getName());
        }

        this.status = TaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        registerEvent(new TaskApprovedEvent(
                String.valueOf(getId()),
                taskCode,
                approverId,
                approverName,
                comment
        ));
    }

    /**
     * 审批拒绝/打回
     */
    public void reject(Long approverId, String approverName, String reason) {
        if (!status.canApprove()) {
            throw new BusinessException("任务状态不允许审批: " + status.getName());
        }

        this.status = TaskStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new TaskRejectedEvent(
                String.valueOf(getId()),
                taskCode,
                approverId,
                approverName,
                reason
        ));
    }

    /**
     * 重新提交（被打回后）
     */
    public TaskSubmission resubmit(Long submitterId, String submitterName, String content, List<Long> attachments) {
        if (status != TaskStatus.REJECTED) {
            throw new BusinessException("只有被打回的任务才能重新提交");
        }

        return submit(submitterId, submitterName, content, attachments);
    }

    /**
     * 设置流程信息
     */
    public void setWorkflowInfo(Long templateId, String processInstanceId, String currentNode, List<Long> approvers) {
        this.workflowTemplateId = templateId;
        this.processInstanceId = processInstanceId;
        this.currentNode = currentNode;
        this.currentApprovers = approvers;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新当前审批节点
     */
    public void updateCurrentNode(String nodeName, List<Long> approvers) {
        this.currentNode = nodeName;
        this.currentApprovers = approvers;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 是否已过期
     */
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && !status.isFinal();
    }

    /**
     * 是否可编辑
     */
    public boolean isEditable() {
        return status == TaskStatus.PENDING;
    }

    /**
     * 是否已完成
     */
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }
}
