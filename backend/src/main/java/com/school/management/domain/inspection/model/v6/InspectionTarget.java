package com.school.management.domain.inspection.model.v6;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V6检查目标实体
 * 代表一个具体的检查对象（组织/场所/用户）
 */
public class InspectionTarget implements Entity<Long> {

    private Long id;
    private Long taskId;

    // 目标信息
    private TargetType targetType;
    private Long targetId;
    private String targetName;
    private String targetCode;

    // 归属信息
    private Long orgUnitId;
    private String orgUnitName;
    private Long classId;
    private String className;

    // 共享场所权重
    private BigDecimal weightRatio;

    // 检查状态
    private TargetStatus status;
    private Long lockedBy;
    private LocalDateTime lockedAt;
    private LocalDateTime completedAt;

    // 分数信息
    private BigDecimal baseScore;
    private BigDecimal finalScore;
    private BigDecimal deductionTotal;
    private BigDecimal bonusTotal;

    // 快照
    private String snapshot; // JSON

    private String skipReason;
    private String remarks;

    // 审计
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected InspectionTarget() {
    }

    private InspectionTarget(Builder builder) {
        this.id = builder.id;
        this.taskId = builder.taskId;
        this.targetType = builder.targetType;
        this.targetId = builder.targetId;
        this.targetName = builder.targetName;
        this.targetCode = builder.targetCode;
        this.orgUnitId = builder.orgUnitId;
        this.orgUnitName = builder.orgUnitName;
        this.classId = builder.classId;
        this.className = builder.className;
        this.weightRatio = builder.weightRatio != null ? builder.weightRatio : new BigDecimal("100.00");
        this.status = builder.status != null ? builder.status : TargetStatus.PENDING;
        this.lockedBy = builder.lockedBy;
        this.lockedAt = builder.lockedAt;
        this.completedAt = builder.completedAt;
        this.baseScore = builder.baseScore != null ? builder.baseScore : new BigDecimal("100.00");
        this.finalScore = builder.finalScore;
        this.deductionTotal = builder.deductionTotal != null ? builder.deductionTotal : BigDecimal.ZERO;
        this.bonusTotal = builder.bonusTotal != null ? builder.bonusTotal : BigDecimal.ZERO;
        this.snapshot = builder.snapshot;
        this.skipReason = builder.skipReason;
        this.remarks = builder.remarks;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    /**
     * 创建新检查目标
     */
    public static InspectionTarget create(Long taskId, TargetType targetType, Long targetId,
                                           String targetName, String targetCode,
                                           Long orgUnitId, String orgUnitName,
                                           Long classId, String className,
                                           BigDecimal weightRatio, BigDecimal baseScore) {
        return builder()
                .taskId(taskId)
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .targetCode(targetCode)
                .orgUnitId(orgUnitId)
                .orgUnitName(orgUnitName)
                .classId(classId)
                .className(className)
                .weightRatio(weightRatio)
                .baseScore(baseScore)
                .status(TargetStatus.PENDING)
                .build();
    }

    /**
     * 锁定目标（开始检查）
     */
    public void lock(Long inspectorId) {
        if (this.status != TargetStatus.PENDING) {
            throw new IllegalStateException("只有待检查的目标才能锁定");
        }
        this.status = TargetStatus.LOCKED;
        this.lockedBy = inspectorId;
        this.lockedAt = LocalDateTime.now();
    }

    /**
     * 解锁目标
     */
    public void unlock() {
        if (this.status != TargetStatus.LOCKED) {
            throw new IllegalStateException("只有锁定中的目标才能解锁");
        }
        this.status = TargetStatus.PENDING;
        this.lockedBy = null;
        this.lockedAt = null;
    }

    /**
     * 完成检查
     */
    public void complete() {
        if (this.status != TargetStatus.LOCKED && this.status != TargetStatus.PENDING) {
            throw new IllegalStateException("目标状态不正确，无法完成");
        }
        calculateFinalScore();
        this.status = TargetStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 跳过检查
     */
    public void skip(String reason) {
        if (this.status == TargetStatus.COMPLETED) {
            throw new IllegalStateException("已完成的目标不能跳过");
        }
        this.status = TargetStatus.SKIPPED;
        this.skipReason = reason;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 添加扣分
     */
    public void addDeduction(BigDecimal deduction) {
        this.deductionTotal = this.deductionTotal.add(deduction);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 移除扣分（申诉通过时）
     */
    public void removeDeduction(BigDecimal deduction) {
        this.deductionTotal = this.deductionTotal.subtract(deduction);
        if (this.deductionTotal.compareTo(BigDecimal.ZERO) < 0) {
            this.deductionTotal = BigDecimal.ZERO;
        }
        calculateFinalScore();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 添加加分
     */
    public void addBonus(BigDecimal bonus) {
        this.bonusTotal = this.bonusTotal.add(bonus);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 计算最终分数
     */
    public void calculateFinalScore() {
        this.finalScore = this.baseScore
                .subtract(this.deductionTotal)
                .add(this.bonusTotal);
        if (this.finalScore.compareTo(BigDecimal.ZERO) < 0) {
            this.finalScore = BigDecimal.ZERO;
        }
    }

    /**
     * 获取加权后的分数
     */
    public BigDecimal getWeightedScore() {
        if (this.finalScore == null || this.weightRatio == null) {
            return BigDecimal.ZERO;
        }
        return this.finalScore.multiply(this.weightRatio).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public String getOrgUnitName() {
        return orgUnitName;
    }

    public Long getClassId() {
        return classId;
    }

    public String getClassName() {
        return className;
    }

    public BigDecimal getWeightRatio() {
        return weightRatio;
    }

    public TargetStatus getStatus() {
        return status;
    }

    public Long getLockedBy() {
        return lockedBy;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public BigDecimal getBaseScore() {
        return baseScore;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public BigDecimal getDeductionTotal() {
        return deductionTotal;
    }

    public BigDecimal getBonusTotal() {
        return bonusTotal;
    }

    public String getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(String snapshot) {
        this.snapshot = snapshot;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long taskId;
        private TargetType targetType;
        private Long targetId;
        private String targetName;
        private String targetCode;
        private Long orgUnitId;
        private String orgUnitName;
        private Long classId;
        private String className;
        private BigDecimal weightRatio;
        private TargetStatus status;
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

        public Builder id(Long id) { this.id = id; return this; }
        public Builder taskId(Long taskId) { this.taskId = taskId; return this; }
        public Builder targetType(TargetType targetType) { this.targetType = targetType; return this; }
        public Builder targetId(Long targetId) { this.targetId = targetId; return this; }
        public Builder targetName(String targetName) { this.targetName = targetName; return this; }
        public Builder targetCode(String targetCode) { this.targetCode = targetCode; return this; }
        public Builder orgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; return this; }
        public Builder orgUnitName(String orgUnitName) { this.orgUnitName = orgUnitName; return this; }
        public Builder classId(Long classId) { this.classId = classId; return this; }
        public Builder className(String className) { this.className = className; return this; }
        public Builder weightRatio(BigDecimal weightRatio) { this.weightRatio = weightRatio; return this; }
        public Builder status(TargetStatus status) { this.status = status; return this; }
        public Builder lockedBy(Long lockedBy) { this.lockedBy = lockedBy; return this; }
        public Builder lockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder baseScore(BigDecimal baseScore) { this.baseScore = baseScore; return this; }
        public Builder finalScore(BigDecimal finalScore) { this.finalScore = finalScore; return this; }
        public Builder deductionTotal(BigDecimal deductionTotal) { this.deductionTotal = deductionTotal; return this; }
        public Builder bonusTotal(BigDecimal bonusTotal) { this.bonusTotal = bonusTotal; return this; }
        public Builder snapshot(String snapshot) { this.snapshot = snapshot; return this; }
        public Builder skipReason(String skipReason) { this.skipReason = skipReason; return this; }
        public Builder remarks(String remarks) { this.remarks = remarks; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public InspectionTarget build() {
            return new InspectionTarget(this);
        }
    }
}
