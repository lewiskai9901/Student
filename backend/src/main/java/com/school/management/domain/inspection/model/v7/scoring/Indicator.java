package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * V7 评价指标 — 聚合根
 * <p>
 * 指标树节点，分 LEAF（叶子，绑定数据来源分区）和 COMPOSITE（复合，聚合子指标）两种类型。
 */
public class Indicator implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private Long parentIndicatorId;
    private String name;
    private String indicatorType; // LEAF | COMPOSITE

    // LEAF fields
    private Long sourceSectionId;
    private String sourceAggregation; // AVG | MAX | MIN | LATEST | SUM

    // COMPOSITE fields
    private String compositeAggregation; // WEIGHTED_AVG | SUM | AVG | MIN | MAX
    private String missingPolicy; // SKIP | CARRY_FORWARD | MARK_INCOMPLETE

    // Normalization
    private String normalization;       // NONE | RELATION_COUNT | FIXED_VALUE | PERCENTAGE
    private String normalizationConfig; // JSON config

    private String evaluationPeriod; // PER_TASK | DAILY | WEEKLY | MONTHLY
    private Long gradeSchemeId;
    private String evaluationMethod;    // SCORE_RANGE | PERCENT_RANGE | RANK_COUNT | RANK_PERCENT
    private String gradeThresholds;     // JSON: [{"gradeCode":"RED","value":90},...]
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Indicator() {
    }

    private Indicator(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.projectId = builder.projectId;
        this.parentIndicatorId = builder.parentIndicatorId;
        this.name = builder.name;
        this.indicatorType = builder.indicatorType;
        this.sourceSectionId = builder.sourceSectionId;
        this.sourceAggregation = builder.sourceAggregation;
        this.compositeAggregation = builder.compositeAggregation;
        this.missingPolicy = builder.missingPolicy;
        this.normalization = builder.normalization != null ? builder.normalization : "NONE";
        this.normalizationConfig = builder.normalizationConfig;
        this.evaluationPeriod = builder.evaluationPeriod != null ? builder.evaluationPeriod : "PER_TASK";
        this.gradeSchemeId = builder.gradeSchemeId;
        this.evaluationMethod = builder.evaluationMethod != null ? builder.evaluationMethod : "PERCENT_RANGE";
        this.gradeThresholds = builder.gradeThresholds;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    // ── Factory ──────────────────────────────────────────────

    public static Indicator createLeaf(Long projectId, String name,
                                       Long sourceSectionId, String sourceAggregation,
                                       String evaluationPeriod, Long gradeSchemeId) {
        Indicator ind = new Indicator();
        ind.projectId = projectId;
        ind.name = name;
        ind.indicatorType = "LEAF";
        ind.sourceSectionId = sourceSectionId;
        ind.sourceAggregation = sourceAggregation != null ? sourceAggregation : "AVG";
        ind.evaluationPeriod = evaluationPeriod != null ? evaluationPeriod : "PER_TASK";
        ind.gradeSchemeId = gradeSchemeId;
        ind.sortOrder = 0;
        ind.createdAt = LocalDateTime.now();
        return ind;
    }

    public static Indicator createComposite(Long projectId, String name,
                                            String compositeAggregation, String missingPolicy,
                                            String evaluationPeriod, Long gradeSchemeId) {
        Indicator ind = new Indicator();
        ind.projectId = projectId;
        ind.name = name;
        ind.indicatorType = "COMPOSITE";
        ind.compositeAggregation = compositeAggregation != null ? compositeAggregation : "AVG";
        ind.missingPolicy = missingPolicy != null ? missingPolicy : "SKIP";
        ind.evaluationPeriod = evaluationPeriod != null ? evaluationPeriod : "WEEKLY";
        ind.gradeSchemeId = gradeSchemeId;
        ind.sortOrder = 0;
        ind.createdAt = LocalDateTime.now();
        return ind;
    }

    public static Indicator reconstruct(Builder builder) {
        return new Indicator(builder);
    }

    // ── Commands ─────────────────────────────────────────────

    public void update(String name, String evaluationPeriod,
                       Long gradeSchemeId, Long sourceSectionId, String sourceAggregation,
                       String compositeAggregation, String missingPolicy,
                       String normalization, String normalizationConfig,
                       String evaluationMethod, String gradeThresholds,
                       Integer sortOrder) {
        this.name = name;
        this.evaluationPeriod = evaluationPeriod;
        this.gradeSchemeId = gradeSchemeId;
        this.sourceSectionId = sourceSectionId;
        this.sourceAggregation = sourceAggregation;
        this.compositeAggregation = compositeAggregation;
        this.missingPolicy = missingPolicy;
        this.normalization = normalization;
        this.normalizationConfig = normalizationConfig;
        this.evaluationMethod = evaluationMethod;
        this.gradeThresholds = gradeThresholds;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void setParentIndicatorId(Long parentIndicatorId) {
        this.parentIndicatorId = parentIndicatorId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    // ── Queries ──────────────────────────────────────────────

    public boolean isLeaf() {
        return "LEAF".equals(indicatorType);
    }

    public boolean isComposite() {
        return "COMPOSITE".equals(indicatorType);
    }

    public boolean isRoot() {
        return parentIndicatorId == null;
    }

    // ── Getters ──────────────────────────────────────────────

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getParentIndicatorId() {
        return parentIndicatorId;
    }

    public String getName() {
        return name;
    }

    public String getIndicatorType() {
        return indicatorType;
    }

    public Long getSourceSectionId() {
        return sourceSectionId;
    }

    public String getSourceAggregation() {
        return sourceAggregation;
    }

    public String getCompositeAggregation() {
        return compositeAggregation;
    }

    public String getMissingPolicy() {
        return missingPolicy;
    }

    public String getNormalization() { return normalization; }
    public String getNormalizationConfig() { return normalizationConfig; }

    public String getEvaluationPeriod() {
        return evaluationPeriod;
    }

    public Long getGradeSchemeId() {
        return gradeSchemeId;
    }

    public String getEvaluationMethod() {
        return evaluationMethod;
    }

    public String getGradeThresholds() {
        return gradeThresholds;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ── Builder ──────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long projectId;
        private Long parentIndicatorId;
        private String name;
        private String indicatorType;
        private Long sourceSectionId;
        private String sourceAggregation;
        private String compositeAggregation;
        private String missingPolicy;
        private String normalization;
        private String normalizationConfig;
        private String evaluationPeriod;
        private Long gradeSchemeId;
        private String evaluationMethod;
        private String gradeThresholds;
        private Integer sortOrder;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder parentIndicatorId(Long parentIndicatorId) { this.parentIndicatorId = parentIndicatorId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder indicatorType(String indicatorType) { this.indicatorType = indicatorType; return this; }
        public Builder sourceSectionId(Long sourceSectionId) { this.sourceSectionId = sourceSectionId; return this; }
        public Builder sourceAggregation(String sourceAggregation) { this.sourceAggregation = sourceAggregation; return this; }
        public Builder compositeAggregation(String compositeAggregation) { this.compositeAggregation = compositeAggregation; return this; }
        public Builder missingPolicy(String missingPolicy) { this.missingPolicy = missingPolicy; return this; }
        public Builder normalization(String normalization) { this.normalization = normalization; return this; }
        public Builder normalizationConfig(String normalizationConfig) { this.normalizationConfig = normalizationConfig; return this; }
        public Builder evaluationPeriod(String evaluationPeriod) { this.evaluationPeriod = evaluationPeriod; return this; }
        public Builder gradeSchemeId(Long gradeSchemeId) { this.gradeSchemeId = gradeSchemeId; return this; }
        public Builder evaluationMethod(String evaluationMethod) { this.evaluationMethod = evaluationMethod; return this; }
        public Builder gradeThresholds(String gradeThresholds) { this.gradeThresholds = gradeThresholds; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Indicator build() {
            return new Indicator(this);
        }
    }
}
