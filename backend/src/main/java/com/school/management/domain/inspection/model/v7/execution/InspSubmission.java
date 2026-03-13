package com.school.management.domain.inspection.model.v7.execution;

import com.school.management.domain.inspection.event.v7.SubmissionCompletedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V7 检查提交聚合根（每个检查目标一个提交）
 * 状态机: PENDING → LOCKED → IN_PROGRESS → COMPLETED
 * 终态: SKIPPED
 */
public class InspSubmission extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private Long taskId;
    private TargetType targetType;
    private Long targetId;
    private String targetName;
    private Long orgUnitId;
    private String orgUnitName;
    private BigDecimal weightRatio;
    private SubmissionStatus status;
    private String formData;
    private String scoreBreakdown;
    private BigDecimal baseScore;
    private BigDecimal finalScore;
    private BigDecimal deductionTotal;
    private BigDecimal bonusTotal;
    private String grade;
    private Boolean passed;
    private Integer totalTimeSeconds;
    private String nfcTagUid;
    private LocalDateTime checkinTime;
    private Integer syncVersion;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected InspSubmission() {
    }

    private InspSubmission(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.taskId = builder.taskId;
        this.targetType = builder.targetType;
        this.targetId = builder.targetId;
        this.targetName = builder.targetName;
        this.orgUnitId = builder.orgUnitId;
        this.orgUnitName = builder.orgUnitName;
        this.weightRatio = builder.weightRatio != null ? builder.weightRatio : BigDecimal.ONE;
        this.status = builder.status != null ? builder.status : SubmissionStatus.PENDING;
        this.formData = builder.formData;
        this.scoreBreakdown = builder.scoreBreakdown;
        this.baseScore = builder.baseScore;
        this.finalScore = builder.finalScore;
        this.deductionTotal = builder.deductionTotal != null ? builder.deductionTotal : BigDecimal.ZERO;
        this.bonusTotal = builder.bonusTotal != null ? builder.bonusTotal : BigDecimal.ZERO;
        this.grade = builder.grade;
        this.passed = builder.passed;
        this.totalTimeSeconds = builder.totalTimeSeconds;
        this.nfcTagUid = builder.nfcTagUid;
        this.checkinTime = builder.checkinTime;
        this.syncVersion = builder.syncVersion != null ? builder.syncVersion : 1;
        this.completedAt = builder.completedAt;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static InspSubmission create(Long taskId, TargetType targetType,
                                        Long targetId, String targetName) {
        return builder()
                .taskId(taskId)
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .status(SubmissionStatus.PENDING)
                .build();
    }

    public static InspSubmission reconstruct(Builder builder) {
        return new InspSubmission(builder);
    }

    /**
     * 锁定提交 — PENDING → LOCKED
     */
    public void lock() {
        if (this.status != SubmissionStatus.PENDING) {
            throw new IllegalStateException("只有待检查的提交才能锁定");
        }
        this.status = SubmissionStatus.LOCKED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 解锁提交 — LOCKED → PENDING
     */
    public void unlock() {
        if (this.status != SubmissionStatus.LOCKED) {
            throw new IllegalStateException("只有已锁定的提交才能解锁");
        }
        this.status = SubmissionStatus.PENDING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 开始填写 — LOCKED → IN_PROGRESS
     */
    public void startFilling() {
        if (this.status != SubmissionStatus.LOCKED && this.status != SubmissionStatus.PENDING) {
            throw new IllegalStateException("只有待检查或已锁定的提交才能开始填写");
        }
        this.status = SubmissionStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 保存表单数据（自动保存）
     */
    public void saveFormData(String formData) {
        if (this.status != SubmissionStatus.IN_PROGRESS && this.status != SubmissionStatus.LOCKED) {
            throw new IllegalStateException("只有进行中或已锁定的提交才能保存");
        }
        this.formData = formData;
        this.syncVersion = this.syncVersion + 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 完成提交 — IN_PROGRESS → COMPLETED
     */
    public void complete(BigDecimal baseScore, BigDecimal finalScore,
                         BigDecimal deductionTotal, BigDecimal bonusTotal,
                         String scoreBreakdown, String grade, Boolean passed) {
        if (this.status != SubmissionStatus.IN_PROGRESS) {
            throw new IllegalStateException("只有进行中的提交才能完成");
        }
        this.baseScore = baseScore;
        this.finalScore = finalScore;
        this.deductionTotal = deductionTotal;
        this.bonusTotal = bonusTotal;
        this.scoreBreakdown = scoreBreakdown;
        this.grade = grade;
        this.passed = passed;
        this.status = SubmissionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        registerEvent(new SubmissionCompletedEvent(this.id, this.taskId,
                this.targetType.name(), this.targetId, this.finalScore));
    }

    /**
     * 跳过提交 — PENDING/LOCKED → SKIPPED
     */
    public void skip() {
        if (this.status != SubmissionStatus.PENDING && this.status != SubmissionStatus.LOCKED) {
            throw new IllegalStateException("只有待检查或已锁定的提交才能跳过");
        }
        this.status = SubmissionStatus.SKIPPED;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getTaskId() { return taskId; }
    public TargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
    public Long getOrgUnitId() { return orgUnitId; }
    public String getOrgUnitName() { return orgUnitName; }
    public BigDecimal getWeightRatio() { return weightRatio; }
    public SubmissionStatus getStatus() { return status; }
    public String getFormData() { return formData; }
    public String getScoreBreakdown() { return scoreBreakdown; }
    public BigDecimal getBaseScore() { return baseScore; }
    public BigDecimal getFinalScore() { return finalScore; }
    public BigDecimal getDeductionTotal() { return deductionTotal; }
    public BigDecimal getBonusTotal() { return bonusTotal; }
    public String getGrade() { return grade; }
    public Boolean getPassed() { return passed; }
    public Integer getTotalTimeSeconds() { return totalTimeSeconds; }
    public String getNfcTagUid() { return nfcTagUid; }
    public LocalDateTime getCheckinTime() { return checkinTime; }
    public Integer getSyncVersion() { return syncVersion; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long taskId;
        private TargetType targetType;
        private Long targetId;
        private String targetName;
        private Long orgUnitId;
        private String orgUnitName;
        private BigDecimal weightRatio;
        private SubmissionStatus status;
        private String formData;
        private String scoreBreakdown;
        private BigDecimal baseScore;
        private BigDecimal finalScore;
        private BigDecimal deductionTotal;
        private BigDecimal bonusTotal;
        private String grade;
        private Boolean passed;
        private Integer totalTimeSeconds;
        private String nfcTagUid;
        private LocalDateTime checkinTime;
        private Integer syncVersion;
        private LocalDateTime completedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder taskId(Long taskId) { this.taskId = taskId; return this; }
        public Builder targetType(TargetType targetType) { this.targetType = targetType; return this; }
        public Builder targetId(Long targetId) { this.targetId = targetId; return this; }
        public Builder targetName(String targetName) { this.targetName = targetName; return this; }
        public Builder orgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; return this; }
        public Builder orgUnitName(String orgUnitName) { this.orgUnitName = orgUnitName; return this; }
        public Builder weightRatio(BigDecimal weightRatio) { this.weightRatio = weightRatio; return this; }
        public Builder status(SubmissionStatus status) { this.status = status; return this; }
        public Builder formData(String formData) { this.formData = formData; return this; }
        public Builder scoreBreakdown(String scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; return this; }
        public Builder baseScore(BigDecimal baseScore) { this.baseScore = baseScore; return this; }
        public Builder finalScore(BigDecimal finalScore) { this.finalScore = finalScore; return this; }
        public Builder deductionTotal(BigDecimal deductionTotal) { this.deductionTotal = deductionTotal; return this; }
        public Builder bonusTotal(BigDecimal bonusTotal) { this.bonusTotal = bonusTotal; return this; }
        public Builder grade(String grade) { this.grade = grade; return this; }
        public Builder passed(Boolean passed) { this.passed = passed; return this; }
        public Builder totalTimeSeconds(Integer totalTimeSeconds) { this.totalTimeSeconds = totalTimeSeconds; return this; }
        public Builder nfcTagUid(String nfcTagUid) { this.nfcTagUid = nfcTagUid; return this; }
        public Builder checkinTime(LocalDateTime checkinTime) { this.checkinTime = checkinTime; return this; }
        public Builder syncVersion(Integer syncVersion) { this.syncVersion = syncVersion; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public InspSubmission build() { return new InspSubmission(this); }
    }
}
