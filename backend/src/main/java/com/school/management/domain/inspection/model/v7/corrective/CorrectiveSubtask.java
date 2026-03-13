package com.school.management.domain.inspection.model.v7.corrective;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V7 整改子任务实体
 * 状态机: PENDING → IN_PROGRESS → COMPLETED
 *         PENDING|IN_PROGRESS → BLOCKED
 */
public class CorrectiveSubtask extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private Long caseId;
    private String subtaskName;
    private String description;
    private Long assigneeId;
    private String status; // PENDING, IN_PROGRESS, COMPLETED, BLOCKED
    private Integer priority;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected CorrectiveSubtask() {
    }

    private CorrectiveSubtask(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.caseId = builder.caseId;
        this.subtaskName = builder.subtaskName;
        this.description = builder.description;
        this.assigneeId = builder.assigneeId;
        this.status = builder.status != null ? builder.status : "PENDING";
        this.priority = builder.priority != null ? builder.priority : 0;
        this.dueDate = builder.dueDate;
        this.completedAt = builder.completedAt;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static CorrectiveSubtask create(Long caseId, String subtaskName,
                                            Long assigneeId, Long createdBy) {
        return builder()
                .caseId(caseId)
                .subtaskName(subtaskName)
                .assigneeId(assigneeId)
                .status("PENDING")
                .createdBy(createdBy)
                .build();
    }

    public static CorrectiveSubtask reconstruct(Builder builder) {
        return new CorrectiveSubtask(builder);
    }

    // ---- 状态转换方法 ----

    /**
     * 开始子任务 — PENDING → IN_PROGRESS
     */
    public void start() {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("只有待处理的子任务才能开始");
        }
        this.status = "IN_PROGRESS";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 完成子任务 — IN_PROGRESS → COMPLETED
     */
    public void complete() {
        if (!"IN_PROGRESS".equals(this.status)) {
            throw new IllegalStateException("只有进行中的子任务才能完成");
        }
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 阻塞子任务 — PENDING|IN_PROGRESS → BLOCKED
     */
    public void block() {
        if ("COMPLETED".equals(this.status) || "BLOCKED".equals(this.status)) {
            throw new IllegalStateException("已完成或已阻塞的子任务不能再阻塞");
        }
        this.status = "BLOCKED";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新子任务详情
     */
    public void updateDetails(String subtaskName, String description,
                              Long assigneeId, Integer priority, LocalDate dueDate) {
        this.subtaskName = subtaskName;
        this.description = description;
        this.assigneeId = assigneeId;
        this.priority = priority;
        this.dueDate = dueDate;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getCaseId() { return caseId; }
    public String getSubtaskName() { return subtaskName; }
    public String getDescription() { return description; }
    public Long getAssigneeId() { return assigneeId; }
    public String getStatus() { return status; }
    public Integer getPriority() { return priority; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public Integer getSortOrder() { return sortOrder; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long caseId;
        private String subtaskName;
        private String description;
        private Long assigneeId;
        private String status;
        private Integer priority;
        private LocalDate dueDate;
        private LocalDateTime completedAt;
        private Integer sortOrder;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder caseId(Long caseId) { this.caseId = caseId; return this; }
        public Builder subtaskName(String subtaskName) { this.subtaskName = subtaskName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder assigneeId(Long assigneeId) { this.assigneeId = assigneeId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder priority(Integer priority) { this.priority = priority; return this; }
        public Builder dueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public CorrectiveSubtask build() { return new CorrectiveSubtask(this); }
    }
}
