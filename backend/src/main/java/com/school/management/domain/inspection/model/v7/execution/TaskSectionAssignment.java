package com.school.management.domain.inspection.model.v7.execution;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * V7 任务分区分配 - 协作检查功能
 * 将检查任务的不同模板分区分配给不同检查员，支持并行检查
 */
public class TaskSectionAssignment implements Entity<Long> {

    private Long id;
    private Long taskId;
    private Long sectionId;
    private Long inspectorId;
    private String status; // PENDING / IN_PROGRESS / COMPLETED
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    protected TaskSectionAssignment() {
    }

    private TaskSectionAssignment(Builder builder) {
        this.id = builder.id;
        this.taskId = builder.taskId;
        this.sectionId = builder.sectionId;
        this.inspectorId = builder.inspectorId;
        this.status = builder.status != null ? builder.status : "PENDING";
        this.startedAt = builder.startedAt;
        this.completedAt = builder.completedAt;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    public static TaskSectionAssignment create(Long taskId, Long sectionId, Long inspectorId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId不能为空");
        }
        if (sectionId == null) {
            throw new IllegalArgumentException("sectionId不能为空");
        }
        if (inspectorId == null) {
            throw new IllegalArgumentException("inspectorId不能为空");
        }
        return builder()
                .taskId(taskId)
                .sectionId(sectionId)
                .inspectorId(inspectorId)
                .build();
    }

    public static TaskSectionAssignment reconstruct(Builder builder) {
        return new TaskSectionAssignment(builder);
    }

    public void start() {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("只有待处理状态的分配才能开始");
        }
        this.status = "IN_PROGRESS";
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        if (!"IN_PROGRESS".equals(this.status)) {
            throw new IllegalStateException("只有进行中状态的分配才能完成");
        }
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public Long getSectionId() { return sectionId; }
    public Long getInspectorId() { return inspectorId; }
    public String getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long taskId;
        private Long sectionId;
        private Long inspectorId;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder taskId(Long taskId) { this.taskId = taskId; return this; }
        public Builder sectionId(Long sectionId) { this.sectionId = sectionId; return this; }
        public Builder inspectorId(Long inspectorId) { this.inspectorId = inspectorId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder startedAt(LocalDateTime startedAt) { this.startedAt = startedAt; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public TaskSectionAssignment build() { return new TaskSectionAssignment(this); }
    }
}
