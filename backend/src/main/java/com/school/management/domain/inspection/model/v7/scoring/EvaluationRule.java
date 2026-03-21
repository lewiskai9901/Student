package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * 评选规则聚合根 — 基于检查数据对目标（组织/场地/人员）进行周期性评选
 * targetType: ORG/PLACE/USER
 * evaluationPeriod: WEEKLY/MONTHLY/QUARTERLY/CUSTOM
 */
public class EvaluationRule extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private String ruleName;
    private String ruleDescription;
    private String targetType;
    private String evaluationPeriod;
    private String awardName;
    private Boolean rankingEnabled;
    private Integer sortOrder;
    private Boolean isEnabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    protected EvaluationRule() {
    }

    private EvaluationRule(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.projectId = builder.projectId;
        this.ruleName = builder.ruleName;
        this.ruleDescription = builder.ruleDescription;
        this.targetType = builder.targetType;
        this.evaluationPeriod = builder.evaluationPeriod != null ? builder.evaluationPeriod : "MONTHLY";
        this.awardName = builder.awardName;
        this.rankingEnabled = builder.rankingEnabled != null ? builder.rankingEnabled : true;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    public static EvaluationRule create(Long projectId, String ruleName, String targetType, Long createdBy) {
        return builder()
                .projectId(projectId)
                .ruleName(ruleName)
                .targetType(targetType)
                .createdBy(createdBy)
                .build();
    }

    public static EvaluationRule reconstruct(Builder builder) {
        return new EvaluationRule(builder);
    }

    public void update(String ruleName, String ruleDescription, String targetType,
                       String evaluationPeriod, String awardName,
                       Boolean rankingEnabled, Integer sortOrder, Long updatedBy) {
        this.ruleName = ruleName;
        this.ruleDescription = ruleDescription;
        this.targetType = targetType;
        this.evaluationPeriod = evaluationPeriod;
        this.awardName = awardName;
        this.rankingEnabled = rankingEnabled;
        this.sortOrder = sortOrder;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void enable() {
        this.isEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.isEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getProjectId() { return projectId; }
    public String getRuleName() { return ruleName; }
    public String getRuleDescription() { return ruleDescription; }
    public String getTargetType() { return targetType; }
    public String getEvaluationPeriod() { return evaluationPeriod; }
    public String getAwardName() { return awardName; }
    public Boolean getRankingEnabled() { return rankingEnabled; }
    public Integer getSortOrder() { return sortOrder; }
    public Boolean getIsEnabled() { return isEnabled; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long projectId;
        private String ruleName;
        private String ruleDescription;
        private String targetType;
        private String evaluationPeriod;
        private String awardName;
        private Boolean rankingEnabled;
        private Integer sortOrder;
        private Boolean isEnabled;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public Builder ruleDescription(String ruleDescription) { this.ruleDescription = ruleDescription; return this; }
        public Builder targetType(String targetType) { this.targetType = targetType; return this; }
        public Builder evaluationPeriod(String evaluationPeriod) { this.evaluationPeriod = evaluationPeriod; return this; }
        public Builder awardName(String awardName) { this.awardName = awardName; return this; }
        public Builder rankingEnabled(Boolean rankingEnabled) { this.rankingEnabled = rankingEnabled; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public EvaluationRule build() { return new EvaluationRule(this); }
    }
}
