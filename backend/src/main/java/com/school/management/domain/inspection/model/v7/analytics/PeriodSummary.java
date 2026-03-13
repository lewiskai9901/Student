package com.school.management.domain.inspection.model.v7.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V7 周期汇总读模型
 * CQRS 读侧 — 从日汇总数据聚合计算
 */
public class PeriodSummary {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private PeriodType periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String targetType;
    private Long targetId;
    private String targetName;
    private Long orgUnitId;
    private String orgUnitName;
    private Integer inspectionDays;
    private BigDecimal avgScore;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private BigDecimal scoreStdDev;
    private TrendDirection trendDirection;
    private BigDecimal trendPercent;
    private Integer ranking;
    private String dimensionScores;
    private String grade;
    private Integer correctiveCount;
    private Integer correctiveClosedCount;
    private BigDecimal prevPeriodScore;
    private BigDecimal momChange;
    private BigDecimal yoyScore;
    private BigDecimal yoyChange;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PeriodSummary() {
    }

    // Getters & Setters
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
    public Integer getInspectionDays() { return inspectionDays; }
    public void setInspectionDays(Integer inspectionDays) { this.inspectionDays = inspectionDays; }
    public BigDecimal getAvgScore() { return avgScore; }
    public void setAvgScore(BigDecimal avgScore) { this.avgScore = avgScore; }
    public BigDecimal getMinScore() { return minScore; }
    public void setMinScore(BigDecimal minScore) { this.minScore = minScore; }
    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
    public BigDecimal getScoreStdDev() { return scoreStdDev; }
    public void setScoreStdDev(BigDecimal scoreStdDev) { this.scoreStdDev = scoreStdDev; }
    public TrendDirection getTrendDirection() { return trendDirection; }
    public void setTrendDirection(TrendDirection trendDirection) { this.trendDirection = trendDirection; }
    public BigDecimal getTrendPercent() { return trendPercent; }
    public void setTrendPercent(BigDecimal trendPercent) { this.trendPercent = trendPercent; }
    public Integer getRanking() { return ranking; }
    public void setRanking(Integer ranking) { this.ranking = ranking; }
    public String getDimensionScores() { return dimensionScores; }
    public void setDimensionScores(String dimensionScores) { this.dimensionScores = dimensionScores; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Integer getCorrectiveCount() { return correctiveCount; }
    public void setCorrectiveCount(Integer correctiveCount) { this.correctiveCount = correctiveCount; }
    public Integer getCorrectiveClosedCount() { return correctiveClosedCount; }
    public void setCorrectiveClosedCount(Integer correctiveClosedCount) { this.correctiveClosedCount = correctiveClosedCount; }
    public BigDecimal getPrevPeriodScore() { return prevPeriodScore; }
    public void setPrevPeriodScore(BigDecimal prevPeriodScore) { this.prevPeriodScore = prevPeriodScore; }
    public BigDecimal getMomChange() { return momChange; }
    public void setMomChange(BigDecimal momChange) { this.momChange = momChange; }
    public BigDecimal getYoyScore() { return yoyScore; }
    public void setYoyScore(BigDecimal yoyScore) { this.yoyScore = yoyScore; }
    public BigDecimal getYoyChange() { return yoyChange; }
    public void setYoyChange(BigDecimal yoyChange) { this.yoyChange = yoyChange; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
