package com.school.management.domain.inspection.model.v7.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V7 日汇总读模型
 * CQRS 读侧 — 由 AnalyticsProjectionService 事件驱动更新
 */
public class DailySummary {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private LocalDate summaryDate;
    private String targetType;
    private Long targetId;
    private String targetName;
    private Long orgUnitId;
    private String orgUnitName;
    private Integer inspectionCount;
    private BigDecimal avgScore;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private BigDecimal totalDeductions;
    private BigDecimal totalBonuses;
    private Integer passCount;
    private Integer failCount;
    private Integer ranking;
    private String dimensionScores;
    private String grade;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DailySummary() {
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public LocalDate getSummaryDate() { return summaryDate; }
    public void setSummaryDate(LocalDate summaryDate) { this.summaryDate = summaryDate; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getOrgUnitName() { return orgUnitName; }
    public void setOrgUnitName(String orgUnitName) { this.orgUnitName = orgUnitName; }
    public Integer getInspectionCount() { return inspectionCount; }
    public void setInspectionCount(Integer inspectionCount) { this.inspectionCount = inspectionCount; }
    public BigDecimal getAvgScore() { return avgScore; }
    public void setAvgScore(BigDecimal avgScore) { this.avgScore = avgScore; }
    public BigDecimal getMinScore() { return minScore; }
    public void setMinScore(BigDecimal minScore) { this.minScore = minScore; }
    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
    public BigDecimal getTotalBonuses() { return totalBonuses; }
    public void setTotalBonuses(BigDecimal totalBonuses) { this.totalBonuses = totalBonuses; }
    public Integer getPassCount() { return passCount; }
    public void setPassCount(Integer passCount) { this.passCount = passCount; }
    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }
    public Integer getRanking() { return ranking; }
    public void setRanking(Integer ranking) { this.ranking = ranking; }
    public String getDimensionScores() { return dimensionScores; }
    public void setDimensionScores(String dimensionScores) { this.dimensionScores = dimensionScores; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
