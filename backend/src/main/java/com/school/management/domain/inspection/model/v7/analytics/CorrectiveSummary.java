package com.school.management.domain.inspection.model.v7.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 整改汇总读模型
 */
public class CorrectiveSummary {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private PeriodType periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer totalCases;
    private Integer openCases;
    private Integer inProgressCases;
    private Integer closedCases;
    private Integer overdueCases;
    private Integer escalatedCases;
    private BigDecimal avgResolutionDays;
    private BigDecimal onTimeRate;
    private Integer effectivenessConfirmed;
    private Integer effectivenessFailed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CorrectiveSummary() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public PeriodType getPeriodType() { return periodType; }
    public void setPeriodType(PeriodType periodType) { this.periodType = periodType; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public Integer getTotalCases() { return totalCases; }
    public void setTotalCases(Integer totalCases) { this.totalCases = totalCases; }
    public Integer getOpenCases() { return openCases; }
    public void setOpenCases(Integer openCases) { this.openCases = openCases; }
    public Integer getInProgressCases() { return inProgressCases; }
    public void setInProgressCases(Integer inProgressCases) { this.inProgressCases = inProgressCases; }
    public Integer getClosedCases() { return closedCases; }
    public void setClosedCases(Integer closedCases) { this.closedCases = closedCases; }
    public Integer getOverdueCases() { return overdueCases; }
    public void setOverdueCases(Integer overdueCases) { this.overdueCases = overdueCases; }
    public Integer getEscalatedCases() { return escalatedCases; }
    public void setEscalatedCases(Integer escalatedCases) { this.escalatedCases = escalatedCases; }
    public BigDecimal getAvgResolutionDays() { return avgResolutionDays; }
    public void setAvgResolutionDays(BigDecimal avgResolutionDays) { this.avgResolutionDays = avgResolutionDays; }
    public BigDecimal getOnTimeRate() { return onTimeRate; }
    public void setOnTimeRate(BigDecimal onTimeRate) { this.onTimeRate = onTimeRate; }
    public Integer getEffectivenessConfirmed() { return effectivenessConfirmed; }
    public void setEffectivenessConfirmed(Integer effectivenessConfirmed) { this.effectivenessConfirmed = effectivenessConfirmed; }
    public Integer getEffectivenessFailed() { return effectivenessFailed; }
    public void setEffectivenessFailed(Integer effectivenessFailed) { this.effectivenessFailed = effectivenessFailed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
