package com.school.management.domain.inspection.model.v6;

import com.school.management.domain.shared.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V6检查任务实体
 * 代表某一天某个时间段的检查任务
 */
public class InspectionTask implements Entity<Long> {

    private Long id;
    private String taskCode;
    private Long projectId;

    // 任务时间
    private LocalDate taskDate;
    private String timeSlot;

    // 状态
    private TaskStatus status;

    // 检查员
    private Long inspectorId;
    private String inspectorName;
    private LocalDateTime claimedAt;

    // 执行信息
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime publishedAt;

    // 统计
    private Integer totalTargets;
    private Integer completedTargets;
    private Integer skippedTargets;

    private String remarks;

    // 审计
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    protected InspectionTask() {
    }

    private InspectionTask(Builder builder) {
        this.id = builder.id;
        this.taskCode = builder.taskCode;
        this.projectId = builder.projectId;
        this.taskDate = builder.taskDate;
        this.timeSlot = builder.timeSlot;
        this.status = builder.status != null ? builder.status : TaskStatus.PENDING;
        this.inspectorId = builder.inspectorId;
        this.inspectorName = builder.inspectorName;
        this.claimedAt = builder.claimedAt;
        this.startedAt = builder.startedAt;
        this.submittedAt = builder.submittedAt;
        this.reviewedAt = builder.reviewedAt;
        this.publishedAt = builder.publishedAt;
        this.totalTargets = builder.totalTargets != null ? builder.totalTargets : 0;
        this.completedTargets = builder.completedTargets != null ? builder.completedTargets : 0;
        this.skippedTargets = builder.skippedTargets != null ? builder.skippedTargets : 0;
        this.remarks = builder.remarks;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    /**
     * 创建新任务
     */
    public static InspectionTask create(String taskCode, Long projectId, LocalDate taskDate,
                                         String timeSlot, Long createdBy) {
        return builder()
                .taskCode(taskCode)
                .projectId(projectId)
                .taskDate(taskDate)
                .timeSlot(timeSlot)
                .status(TaskStatus.PENDING)
                .createdBy(createdBy)
                .build();
    }

    /**
     * 领取任务
     */
    public void claim(Long inspectorId, String inspectorName) {
        if (this.status != TaskStatus.PENDING) {
            throw new IllegalStateException("只有待执行的任务才能领取");
        }
        if (this.inspectorId != null && !this.inspectorId.equals(inspectorId)) {
            throw new IllegalStateException("该任务已被其他人领取");
        }
        this.inspectorId = inspectorId;
        this.inspectorName = inspectorName;
        this.claimedAt = LocalDateTime.now();
    }

    /**
     * 开始执行
     */
    public void start() {
        if (this.status != TaskStatus.PENDING) {
            throw new IllegalStateException("只有待执行的任务才能开始");
        }
        if (this.inspectorId == null) {
            throw new IllegalStateException("任务必须先被领取才能开始");
        }
        this.status = TaskStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 提交任务
     */
    public void submit() {
        if (this.status != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("只有进行中的任务才能提交");
        }
        this.status = TaskStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    /**
     * 审核通过
     */
    public void review() {
        if (this.status != TaskStatus.SUBMITTED) {
            throw new IllegalStateException("只有已提交的任务才能审核");
        }
        this.status = TaskStatus.REVIEWED;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * 发布任务结果
     */
    public void publish() {
        if (this.status != TaskStatus.REVIEWED && this.status != TaskStatus.SUBMITTED) {
            throw new IllegalStateException("只有已审核或已提交的任务才能发布");
        }
        this.status = TaskStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * 取消任务
     */
    public void cancel() {
        if (this.status == TaskStatus.PUBLISHED) {
            throw new IllegalStateException("已发布的任务不能取消");
        }
        this.status = TaskStatus.CANCELLED;
    }

    /**
     * 更新目标完成统计
     */
    public void updateTargetStats(int totalTargets, int completedTargets, int skippedTargets) {
        this.totalTargets = totalTargets;
        this.completedTargets = completedTargets;
        this.skippedTargets = skippedTargets;
    }

    /**
     * 增加已完成目标数
     */
    public void incrementCompletedTargets() {
        this.completedTargets = (this.completedTargets != null ? this.completedTargets : 0) + 1;
    }

    /**
     * 增加跳过目标数
     */
    public void incrementSkippedTargets() {
        this.skippedTargets = (this.skippedTargets != null ? this.skippedTargets : 0) + 1;
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public Long getProjectId() {
        return projectId;
    }

    public LocalDate getTaskDate() {
        return taskDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Long getInspectorId() {
        return inspectorId;
    }

    public String getInspectorName() {
        return inspectorName;
    }

    public LocalDateTime getClaimedAt() {
        return claimedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public Integer getTotalTargets() {
        return totalTargets;
    }

    public Integer getCompletedTargets() {
        return completedTargets;
    }

    public Integer getSkippedTargets() {
        return skippedTargets;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String taskCode;
        private Long projectId;
        private LocalDate taskDate;
        private String timeSlot;
        private TaskStatus status;
        private Long inspectorId;
        private String inspectorName;
        private LocalDateTime claimedAt;
        private LocalDateTime startedAt;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;
        private LocalDateTime publishedAt;
        private Integer totalTargets;
        private Integer completedTargets;
        private Integer skippedTargets;
        private String remarks;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder taskCode(String taskCode) { this.taskCode = taskCode; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder taskDate(LocalDate taskDate) { this.taskDate = taskDate; return this; }
        public Builder timeSlot(String timeSlot) { this.timeSlot = timeSlot; return this; }
        public Builder status(TaskStatus status) { this.status = status; return this; }
        public Builder inspectorId(Long inspectorId) { this.inspectorId = inspectorId; return this; }
        public Builder inspectorName(String inspectorName) { this.inspectorName = inspectorName; return this; }
        public Builder claimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; return this; }
        public Builder startedAt(LocalDateTime startedAt) { this.startedAt = startedAt; return this; }
        public Builder submittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; return this; }
        public Builder reviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; return this; }
        public Builder publishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; return this; }
        public Builder totalTargets(Integer totalTargets) { this.totalTargets = totalTargets; return this; }
        public Builder completedTargets(Integer completedTargets) { this.completedTargets = completedTargets; return this; }
        public Builder skippedTargets(Integer skippedTargets) { this.skippedTargets = skippedTargets; return this; }
        public Builder remarks(String remarks) { this.remarks = remarks; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public InspectionTask build() {
            return new InspectionTask(this);
        }
    }
}
