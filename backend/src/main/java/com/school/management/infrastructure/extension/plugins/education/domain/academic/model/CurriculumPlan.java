package com.school.management.domain.academic.model;

import com.school.management.domain.shared.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 培养方案聚合根
 */
public class CurriculumPlan extends AggregateRoot<Long> {

    private Long id;

    /** 方案编码 */
    private String planCode;

    /** 方案名称 */
    private String planName;

    /** 所属专业ID */
    private Long majorId;

    /** 所属专业方向ID */
    private Long majorDirectionId;

    /** 适用年级 */
    private Integer gradeYear;

    /** 总学分 */
    private BigDecimal totalCredits;

    /** 必修学分 */
    private BigDecimal requiredCredits;

    /** 选修学分 */
    private BigDecimal electiveCredits;

    /** 实践学分 */
    private BigDecimal practiceCredits;

    /** 培养目标 */
    private String trainingObjective;

    /** 毕业要求 */
    private String graduationRequirement;

    /** 版本号 */
    private Integer planVersion;

    /** 状态: 0-草稿 1-已发布 2-已归档 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    /** 发布人 */
    private Long publishedBy;

    // 审计字段
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected CurriculumPlan() {}

    private CurriculumPlan(Builder builder) {
        this.id = builder.id;
        this.planCode = Objects.requireNonNull(builder.planCode, "planCode cannot be null");
        this.planName = Objects.requireNonNull(builder.planName, "planName cannot be null");
        this.majorId = builder.majorId;
        this.majorDirectionId = builder.majorDirectionId;
        this.gradeYear = builder.gradeYear;
        this.totalCredits = builder.totalCredits;
        this.requiredCredits = builder.requiredCredits;
        this.electiveCredits = builder.electiveCredits;
        this.practiceCredits = builder.practiceCredits;
        this.trainingObjective = builder.trainingObjective;
        this.graduationRequirement = builder.graduationRequirement;
        this.planVersion = builder.planVersion != null ? builder.planVersion : 1;
        this.status = builder.status != null ? builder.status : 0;
        this.publishedAt = builder.publishedAt;
        this.publishedBy = builder.publishedBy;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : this.createdAt;
        this.updatedBy = builder.updatedBy;

        validate();
    }

    /**
     * 工厂方法：创建培养方案
     */
    public static CurriculumPlan create(String planCode, String planName, Long createdBy) {
        return builder()
            .planCode(planCode)
            .planName(planName)
            .createdBy(createdBy)
            .build();
    }

    /**
     * 更新方案信息
     */
    public void update(String planName, Long majorId, Long majorDirectionId, Integer gradeYear,
                       BigDecimal totalCredits, BigDecimal requiredCredits,
                       BigDecimal electiveCredits, BigDecimal practiceCredits,
                       String trainingObjective, String graduationRequirement,
                       Long updatedBy) {
        if (planName != null) this.planName = planName;
        this.majorId = majorId;
        this.majorDirectionId = majorDirectionId;
        if (gradeYear != null) this.gradeYear = gradeYear;
        this.totalCredits = totalCredits;
        this.requiredCredits = requiredCredits;
        this.electiveCredits = electiveCredits;
        this.practiceCredits = practiceCredits;
        if (trainingObjective != null) this.trainingObjective = trainingObjective;
        if (graduationRequirement != null) this.graduationRequirement = graduationRequirement;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 发布方案
     */
    public void publish(Long publishedBy) {
        this.status = 1;
        this.publishedAt = LocalDateTime.now();
        this.publishedBy = publishedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 归档方案
     */
    public void deprecate() {
        this.status = 2;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 创建副本（用于 copyPlan）
     */
    public CurriculumPlan copyWithNewVersion(int newVersion, Long createdBy) {
        return builder()
            .planCode(this.planCode)
            .planName(this.planName)
            .majorId(this.majorId)
            .majorDirectionId(this.majorDirectionId)
            .gradeYear(this.gradeYear)
            .totalCredits(this.totalCredits)
            .requiredCredits(this.requiredCredits)
            .electiveCredits(this.electiveCredits)
            .practiceCredits(this.practiceCredits)
            .trainingObjective(this.trainingObjective)
            .graduationRequirement(this.graduationRequirement)
            .planVersion(newVersion)
            .status(0) // draft
            .createdBy(createdBy)
            .build();
    }

    private void validate() {
        if (planCode == null || planCode.isBlank()) {
            throw new IllegalArgumentException("Plan code cannot be empty");
        }
        if (planCode.length() > 50) {
            throw new IllegalArgumentException("Plan code cannot exceed 50 characters");
        }
        if (planName == null || planName.isBlank()) {
            throw new IllegalArgumentException("Plan name cannot be empty");
        }
        if (planName.length() > 100) {
            throw new IllegalArgumentException("Plan name cannot exceed 100 characters");
        }
    }

    // Getters
    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlanCode() { return planCode; }
    public String getPlanName() { return planName; }
    public Long getMajorId() { return majorId; }
    public Long getMajorDirectionId() { return majorDirectionId; }
    public Integer getGradeYear() { return gradeYear; }
    public BigDecimal getTotalCredits() { return totalCredits; }
    public BigDecimal getRequiredCredits() { return requiredCredits; }
    public BigDecimal getElectiveCredits() { return electiveCredits; }
    public BigDecimal getPracticeCredits() { return practiceCredits; }
    public String getTrainingObjective() { return trainingObjective; }
    public String getGraduationRequirement() { return graduationRequirement; }
    public Integer getPlanVersion() { return planVersion; }
    public Integer getStatus() { return status; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public Long getPublishedBy() { return publishedBy; }
    public Long getCreatedBy() { return createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String planCode;
        private String planName;
        private Long majorId;
        private Long majorDirectionId;
        private Integer gradeYear;
        private BigDecimal totalCredits;
        private BigDecimal requiredCredits;
        private BigDecimal electiveCredits;
        private BigDecimal practiceCredits;
        private String trainingObjective;
        private String graduationRequirement;
        private Integer planVersion;
        private Integer status;
        private LocalDateTime publishedAt;
        private Long publishedBy;
        private Long createdBy;
        private Long updatedBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder planCode(String v) { this.planCode = v; return this; }
        public Builder planName(String v) { this.planName = v; return this; }
        public Builder majorId(Long v) { this.majorId = v; return this; }
        public Builder majorDirectionId(Long v) { this.majorDirectionId = v; return this; }
        public Builder gradeYear(Integer v) { this.gradeYear = v; return this; }
        public Builder totalCredits(BigDecimal v) { this.totalCredits = v; return this; }
        public Builder requiredCredits(BigDecimal v) { this.requiredCredits = v; return this; }
        public Builder electiveCredits(BigDecimal v) { this.electiveCredits = v; return this; }
        public Builder practiceCredits(BigDecimal v) { this.practiceCredits = v; return this; }
        public Builder trainingObjective(String v) { this.trainingObjective = v; return this; }
        public Builder graduationRequirement(String v) { this.graduationRequirement = v; return this; }
        public Builder planVersion(Integer v) { this.planVersion = v; return this; }
        public Builder status(Integer v) { this.status = v; return this; }
        public Builder publishedAt(LocalDateTime v) { this.publishedAt = v; return this; }
        public Builder publishedBy(Long v) { this.publishedBy = v; return this; }
        public Builder createdBy(Long v) { this.createdBy = v; return this; }
        public Builder updatedBy(Long v) { this.updatedBy = v; return this; }
        public Builder createdAt(LocalDateTime v) { this.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v) { this.updatedAt = v; return this; }

        public CurriculumPlan build() { return new CurriculumPlan(this); }
    }
}
