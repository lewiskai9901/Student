package com.school.management.domain.inspection.model.v7.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Item 频次汇总读模型（Pareto 分析用）
 */
public class ItemFrequencySummary {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private PeriodType periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String itemCode;
    private String itemName;
    private Long sectionId;
    private String sectionName;
    private Integer occurrenceCount;
    private Integer flaggedCount;
    private BigDecimal totalDeduction;
    private BigDecimal avgDeduction;
    private BigDecimal cumulativePercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ItemFrequencySummary() {}

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
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }
    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }
    public Integer getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(Integer occurrenceCount) { this.occurrenceCount = occurrenceCount; }
    public Integer getFlaggedCount() { return flaggedCount; }
    public void setFlaggedCount(Integer flaggedCount) { this.flaggedCount = flaggedCount; }
    public BigDecimal getTotalDeduction() { return totalDeduction; }
    public void setTotalDeduction(BigDecimal totalDeduction) { this.totalDeduction = totalDeduction; }
    public BigDecimal getAvgDeduction() { return avgDeduction; }
    public void setAvgDeduction(BigDecimal avgDeduction) { this.avgDeduction = avgDeduction; }
    public BigDecimal getCumulativePercentage() { return cumulativePercentage; }
    public void setCumulativePercentage(BigDecimal cumulativePercentage) { this.cumulativePercentage = cumulativePercentage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
