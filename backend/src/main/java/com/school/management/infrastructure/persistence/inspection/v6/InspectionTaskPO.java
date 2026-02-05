package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V6检查任务持久化对象
 */
@TableName("inspection_tasks")
public class InspectionTaskPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskCode;
    private Long projectId;

    // 任务时间
    private LocalDate taskDate;
    private String timeSlot;

    // 状态
    private String status;

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

    @TableLogic
    private Integer deleted;

    // 关联查询字段
    @TableField(exist = false)
    private String projectName;

    // Getters
    public Long getId() { return id; }
    public String getTaskCode() { return taskCode; }
    public Long getProjectId() { return projectId; }
    public LocalDate getTaskDate() { return taskDate; }
    public String getTimeSlot() { return timeSlot; }
    public String getStatus() { return status; }
    public Long getInspectorId() { return inspectorId; }
    public String getInspectorName() { return inspectorName; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public Integer getTotalTargets() { return totalTargets; }
    public Integer getCompletedTargets() { return completedTargets; }
    public Integer getSkippedTargets() { return skippedTargets; }
    public String getRemarks() { return remarks; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Integer getDeleted() { return deleted; }
    public String getProjectName() { return projectName; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setTaskDate(LocalDate taskDate) { this.taskDate = taskDate; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public void setStatus(String status) { this.status = status; }
    public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
    public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
    public void setClaimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public void setTotalTargets(Integer totalTargets) { this.totalTargets = totalTargets; }
    public void setCompletedTargets(Integer completedTargets) { this.completedTargets = completedTargets; }
    public void setSkippedTargets(Integer skippedTargets) { this.skippedTargets = skippedTargets; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
}
