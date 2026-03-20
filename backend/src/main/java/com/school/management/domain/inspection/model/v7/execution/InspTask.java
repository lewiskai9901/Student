package com.school.management.domain.inspection.model.v7.execution;

import com.school.management.domain.inspection.event.v7.*;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * V7 检查任务聚合根
 * 状态机: PENDING → CLAIMED → IN_PROGRESS → SUBMITTED → UNDER_REVIEW → REVIEWED → PUBLISHED
 * 终态: CANCELLED, EXPIRED
 */
public class InspTask extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private String taskCode;
    private Long projectId;
    private LocalDate taskDate;
    private String timeSlotCode;
    private LocalTime timeSlotStart;
    private LocalTime timeSlotEnd;
    private Long inspectorId;
    private String inspectorName;
    private Long reviewerId;
    private String reviewerName;
    private TaskStatus status;
    private Integer totalTargets;
    private Integer completedTargets;
    private Integer skippedTargets;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime publishedAt;
    private String reviewComment;
    private String collaborationMode;      // SINGLE / COLLABORATIVE
    private LocalDateTime executionStartedAt;
    private LocalDateTime executionEndedAt;
    private String assignedSectionIds;     // JSON: 分配的分区ID（空=全部）
    private String assignedTargetIds;      // JSON: 分配的目标ID（空=全部）
    private Long inspectionPlanId;         // 关联的检查计划
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected InspTask() {
    }

    private InspTask(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.taskCode = builder.taskCode;
        this.projectId = builder.projectId;
        this.taskDate = builder.taskDate;
        this.timeSlotCode = builder.timeSlotCode;
        this.timeSlotStart = builder.timeSlotStart;
        this.timeSlotEnd = builder.timeSlotEnd;
        this.inspectorId = builder.inspectorId;
        this.inspectorName = builder.inspectorName;
        this.reviewerId = builder.reviewerId;
        this.reviewerName = builder.reviewerName;
        this.status = builder.status != null ? builder.status : TaskStatus.PENDING;
        this.totalTargets = builder.totalTargets != null ? builder.totalTargets : 0;
        this.completedTargets = builder.completedTargets != null ? builder.completedTargets : 0;
        this.skippedTargets = builder.skippedTargets != null ? builder.skippedTargets : 0;
        this.submittedAt = builder.submittedAt;
        this.reviewedAt = builder.reviewedAt;
        this.publishedAt = builder.publishedAt;
        this.reviewComment = builder.reviewComment;
        this.collaborationMode = builder.collaborationMode != null ? builder.collaborationMode : "SINGLE";
        this.executionStartedAt = builder.executionStartedAt;
        this.executionEndedAt = builder.executionEndedAt;
        this.assignedSectionIds = builder.assignedSectionIds;
        this.assignedTargetIds = builder.assignedTargetIds;
        this.inspectionPlanId = builder.inspectionPlanId;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static InspTask create(String taskCode, Long projectId, LocalDate taskDate) {
        InspTask task = builder()
                .taskCode(taskCode)
                .projectId(projectId)
                .taskDate(taskDate)
                .status(TaskStatus.PENDING)
                .build();
        task.registerEvent(new TaskCreatedEvent(null, taskCode, projectId, taskDate));
        return task;
    }

    public static InspTask reconstruct(Builder builder) {
        return new InspTask(builder);
    }

    /**
     * 领取任务 — PENDING → CLAIMED
     */
    public void claim(Long inspectorId, String inspectorName) {
        if (this.status != TaskStatus.PENDING) {
            throw new IllegalStateException("只有待领取的任务才能被领取");
        }
        this.inspectorId = inspectorId;
        this.inspectorName = inspectorName;
        this.status = TaskStatus.CLAIMED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new TaskClaimedEvent(this.id, this.taskCode, inspectorId));
    }

    /**
     * 开始检查 — CLAIMED → IN_PROGRESS
     */
    public void start() {
        if (this.status != TaskStatus.CLAIMED) {
            throw new IllegalStateException("只有已领取的任务才能开始检查");
        }
        this.status = TaskStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new TaskStartedEvent(this.id, this.taskCode));
    }

    /**
     * 提交任务 — IN_PROGRESS → SUBMITTED
     */
    public void submit() {
        if (this.status != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("只有进行中的任务才能提交");
        }
        this.status = TaskStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        registerEvent(new TaskSubmittedEvent(this.id, this.taskCode, this.inspectorId, this.projectId));
    }

    /**
     * 开始审核 — SUBMITTED → UNDER_REVIEW
     */
    public void startReview(Long reviewerId, String reviewerName) {
        if (this.status != TaskStatus.SUBMITTED) {
            throw new IllegalStateException("只有已提交的任务才能审核");
        }
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.status = TaskStatus.UNDER_REVIEW;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 审核完成 — UNDER_REVIEW → REVIEWED
     */
    public void review(String comment) {
        if (this.status != TaskStatus.UNDER_REVIEW) {
            throw new IllegalStateException("只有审核中的任务才能完成审核");
        }
        this.reviewComment = comment;
        this.status = TaskStatus.REVIEWED;
        this.reviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        registerEvent(new TaskReviewedEvent(this.id, this.taskCode, this.reviewerId));
    }

    /**
     * 发布结果 — REVIEWED → PUBLISHED (或 SUBMITTED → PUBLISHED 当无需审核)
     */
    public void publish() {
        if (this.status != TaskStatus.REVIEWED && this.status != TaskStatus.SUBMITTED) {
            throw new IllegalStateException("只有已审核或已提交的任务才能发布");
        }
        this.status = TaskStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        registerEvent(new TaskPublishedEvent(this.id, this.taskCode, this.projectId));
    }

    /**
     * 取消任务
     */
    public void cancel() {
        if (this.status == TaskStatus.PUBLISHED || this.status == TaskStatus.CANCELLED) {
            throw new IllegalStateException("已发布或已取消的任务不能取消");
        }
        this.status = TaskStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new TaskCancelledEvent(this.id, this.taskCode));
    }

    /**
     * 过期任务
     */
    public void expire() {
        if (this.status != TaskStatus.PENDING && this.status != TaskStatus.CLAIMED) {
            throw new IllegalStateException("只有待领取或已领取的任务才能过期");
        }
        this.status = TaskStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new TaskExpiredEvent(this.id, this.taskCode));
    }

    /**
     * 分配检查员
     */
    public void assign(Long inspectorId, String inspectorName) {
        if (this.status != TaskStatus.PENDING) {
            throw new IllegalStateException("只有待领取的任务才能分配检查员");
        }
        this.inspectorId = inspectorId;
        this.inspectorName = inspectorName;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新目标计数
     */
    public void updateTargetCounts(int totalTargets, int completedTargets, int skippedTargets) {
        this.totalTargets = totalTargets;
        this.completedTargets = completedTargets;
        this.skippedTargets = skippedTargets;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public String getTaskCode() { return taskCode; }
    public Long getProjectId() { return projectId; }
    public LocalDate getTaskDate() { return taskDate; }
    public String getTimeSlotCode() { return timeSlotCode; }
    public LocalTime getTimeSlotStart() { return timeSlotStart; }
    public LocalTime getTimeSlotEnd() { return timeSlotEnd; }
    public Long getInspectorId() { return inspectorId; }
    public String getInspectorName() { return inspectorName; }
    public Long getReviewerId() { return reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public TaskStatus getStatus() { return status; }
    public Integer getTotalTargets() { return totalTargets; }
    public Integer getCompletedTargets() { return completedTargets; }
    public Integer getSkippedTargets() { return skippedTargets; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public String getReviewComment() { return reviewComment; }
    public String getCollaborationMode() { return collaborationMode; }
    public LocalDateTime getExecutionStartedAt() { return executionStartedAt; }
    public LocalDateTime getExecutionEndedAt() { return executionEndedAt; }
    public String getAssignedSectionIds() { return assignedSectionIds; }
    public String getAssignedTargetIds() { return assignedTargetIds; }
    public Long getInspectionPlanId() { return inspectionPlanId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private String taskCode;
        private Long projectId;
        private LocalDate taskDate;
        private String timeSlotCode;
        private LocalTime timeSlotStart;
        private LocalTime timeSlotEnd;
        private Long inspectorId;
        private String inspectorName;
        private Long reviewerId;
        private String reviewerName;
        private TaskStatus status;
        private Integer totalTargets;
        private Integer completedTargets;
        private Integer skippedTargets;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;
        private LocalDateTime publishedAt;
        private String reviewComment;
        private String collaborationMode;
        private LocalDateTime executionStartedAt;
        private LocalDateTime executionEndedAt;
        private String assignedSectionIds;
        private String assignedTargetIds;
        private Long inspectionPlanId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder taskCode(String taskCode) { this.taskCode = taskCode; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder taskDate(LocalDate taskDate) { this.taskDate = taskDate; return this; }
        public Builder timeSlotCode(String timeSlotCode) { this.timeSlotCode = timeSlotCode; return this; }
        public Builder timeSlotStart(LocalTime timeSlotStart) { this.timeSlotStart = timeSlotStart; return this; }
        public Builder timeSlotEnd(LocalTime timeSlotEnd) { this.timeSlotEnd = timeSlotEnd; return this; }
        public Builder inspectorId(Long inspectorId) { this.inspectorId = inspectorId; return this; }
        public Builder inspectorName(String inspectorName) { this.inspectorName = inspectorName; return this; }
        public Builder reviewerId(Long reviewerId) { this.reviewerId = reviewerId; return this; }
        public Builder reviewerName(String reviewerName) { this.reviewerName = reviewerName; return this; }
        public Builder status(TaskStatus status) { this.status = status; return this; }
        public Builder totalTargets(Integer totalTargets) { this.totalTargets = totalTargets; return this; }
        public Builder completedTargets(Integer completedTargets) { this.completedTargets = completedTargets; return this; }
        public Builder skippedTargets(Integer skippedTargets) { this.skippedTargets = skippedTargets; return this; }
        public Builder submittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; return this; }
        public Builder reviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; return this; }
        public Builder publishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; return this; }
        public Builder reviewComment(String reviewComment) { this.reviewComment = reviewComment; return this; }
        public Builder collaborationMode(String collaborationMode) { this.collaborationMode = collaborationMode; return this; }
        public Builder executionStartedAt(LocalDateTime executionStartedAt) { this.executionStartedAt = executionStartedAt; return this; }
        public Builder executionEndedAt(LocalDateTime executionEndedAt) { this.executionEndedAt = executionEndedAt; return this; }
        public Builder assignedSectionIds(String assignedSectionIds) { this.assignedSectionIds = assignedSectionIds; return this; }
        public Builder assignedTargetIds(String assignedTargetIds) { this.assignedTargetIds = assignedTargetIds; return this; }
        public Builder inspectionPlanId(Long inspectionPlanId) { this.inspectionPlanId = inspectionPlanId; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public InspTask build() { return new InspTask(this); }
    }
}
