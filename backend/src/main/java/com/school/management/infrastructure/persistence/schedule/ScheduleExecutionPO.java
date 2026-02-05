package com.school.management.infrastructure.persistence.schedule;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Persistence object for schedule executions.
 */
@TableName("schedule_executions")
public class ScheduleExecutionPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long policyId;

    private LocalDate executionDate;

    private String assignedInspectors;  // JSON string

    private Long sessionId;

    private String status;

    private String failureReason;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;

    // Getters
    public Long getId() { return id; }
    public Long getPolicyId() { return policyId; }
    public LocalDate getExecutionDate() { return executionDate; }
    public String getAssignedInspectors() { return assignedInspectors; }
    public Long getSessionId() { return sessionId; }
    public String getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Integer getDeleted() { return deleted; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public void setExecutionDate(LocalDate executionDate) { this.executionDate = executionDate; }
    public void setAssignedInspectors(String assignedInspectors) { this.assignedInspectors = assignedInspectors; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public void setStatus(String status) { this.status = status; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
