package com.school.management.domain.inspection.model.v7.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 检查员绩效汇总读模型
 */
public class InspectorSummary {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private Long inspectorId;
    private String inspectorName;
    private PeriodType periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer cancelledTasks;
    private Integer expiredTasks;
    private BigDecimal avgCompletionTimeMinutes;
    private BigDecimal avgScore;
    private Integer totalSubmissions;
    private Integer flaggedSubmissions;
    private BigDecimal complianceRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InspectorSummary() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getInspectorId() { return inspectorId; }
    public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
    public String getInspectorName() { return inspectorName; }
    public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
    public PeriodType getPeriodType() { return periodType; }
    public void setPeriodType(PeriodType periodType) { this.periodType = periodType; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public Integer getTotalTasks() { return totalTasks; }
    public void setTotalTasks(Integer totalTasks) { this.totalTasks = totalTasks; }
    public Integer getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(Integer completedTasks) { this.completedTasks = completedTasks; }
    public Integer getCancelledTasks() { return cancelledTasks; }
    public void setCancelledTasks(Integer cancelledTasks) { this.cancelledTasks = cancelledTasks; }
    public Integer getExpiredTasks() { return expiredTasks; }
    public void setExpiredTasks(Integer expiredTasks) { this.expiredTasks = expiredTasks; }
    public BigDecimal getAvgCompletionTimeMinutes() { return avgCompletionTimeMinutes; }
    public void setAvgCompletionTimeMinutes(BigDecimal avgCompletionTimeMinutes) { this.avgCompletionTimeMinutes = avgCompletionTimeMinutes; }
    public BigDecimal getAvgScore() { return avgScore; }
    public void setAvgScore(BigDecimal avgScore) { this.avgScore = avgScore; }
    public Integer getTotalSubmissions() { return totalSubmissions; }
    public void setTotalSubmissions(Integer totalSubmissions) { this.totalSubmissions = totalSubmissions; }
    public Integer getFlaggedSubmissions() { return flaggedSubmissions; }
    public void setFlaggedSubmissions(Integer flaggedSubmissions) { this.flaggedSubmissions = flaggedSubmissions; }
    public BigDecimal getComplianceRate() { return complianceRate; }
    public void setComplianceRate(BigDecimal complianceRate) { this.complianceRate = complianceRate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
