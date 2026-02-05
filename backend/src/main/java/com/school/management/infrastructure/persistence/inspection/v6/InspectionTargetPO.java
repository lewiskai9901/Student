package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V6 Inspection Target Persistence Object
 */
@TableName("inspection_targets")
public class InspectionTargetPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String targetType;
    private Long targetId;
    private String targetName;
    private String targetCode;

    private Long orgUnitId;
    private String orgUnitName;
    private Long classId;
    private String className;

    private BigDecimal weightRatio;

    private String status;
    private Long lockedBy;
    private LocalDateTime lockedAt;
    private LocalDateTime completedAt;

    private BigDecimal baseScore;
    private BigDecimal finalScore;
    private BigDecimal deductionTotal;
    private BigDecimal bonusTotal;

    private String snapshot;

    private String skipReason;
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters
    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
    public String getTargetCode() { return targetCode; }
    public Long getOrgUnitId() { return orgUnitId; }
    public String getOrgUnitName() { return orgUnitName; }
    public Long getClassId() { return classId; }
    public String getClassName() { return className; }
    public BigDecimal getWeightRatio() { return weightRatio; }
    public String getStatus() { return status; }
    public Long getLockedBy() { return lockedBy; }
    public LocalDateTime getLockedAt() { return lockedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public BigDecimal getBaseScore() { return baseScore; }
    public BigDecimal getFinalScore() { return finalScore; }
    public BigDecimal getDeductionTotal() { return deductionTotal; }
    public BigDecimal getBonusTotal() { return bonusTotal; }
    public String getSnapshot() { return snapshot; }
    public String getSkipReason() { return skipReason; }
    public String getRemarks() { return remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public void setTargetCode(String targetCode) { this.targetCode = targetCode; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public void setOrgUnitName(String orgUnitName) { this.orgUnitName = orgUnitName; }
    public void setClassId(Long classId) { this.classId = classId; }
    public void setClassName(String className) { this.className = className; }
    public void setWeightRatio(BigDecimal weightRatio) { this.weightRatio = weightRatio; }
    public void setStatus(String status) { this.status = status; }
    public void setLockedBy(Long lockedBy) { this.lockedBy = lockedBy; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public void setBaseScore(BigDecimal baseScore) { this.baseScore = baseScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }
    public void setDeductionTotal(BigDecimal deductionTotal) { this.deductionTotal = deductionTotal; }
    public void setBonusTotal(BigDecimal bonusTotal) { this.bonusTotal = bonusTotal; }
    public void setSnapshot(String snapshot) { this.snapshot = snapshot; }
    public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
