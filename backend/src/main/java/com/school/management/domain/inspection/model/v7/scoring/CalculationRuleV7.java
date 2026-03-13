package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V7 计算规则
 */
public class CalculationRuleV7 implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long scoringProfileId;
    private String ruleCode;
    private String ruleName;
    private Integer priority;
    private RuleType ruleType;
    private String config;
    private Boolean isEnabled;
    private String scopeType;           // GLOBAL, DIMENSION, CROSS_DIMENSION
    private String targetDimensionIds;  // JSON array of dimension IDs
    private String activationCondition; // 1.4: JSON条件逻辑V2格式
    private String appliesTo;           // 1.4: JSON {targetTypes:[], orgUnitIds:[], userTypes:[]}
    private LocalDate effectiveFrom;    // 1.5: 生效起始日
    private LocalDate effectiveUntil;   // 1.5: 生效截止日
    private String exclusionGroup;      // 1.6: 互斥组名
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected CalculationRuleV7() {
    }

    private CalculationRuleV7(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.scoringProfileId = builder.scoringProfileId;
        this.ruleCode = builder.ruleCode;
        this.ruleName = builder.ruleName;
        this.priority = builder.priority != null ? builder.priority : 0;
        this.ruleType = builder.ruleType;
        this.config = builder.config;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.scopeType = builder.scopeType != null ? builder.scopeType : "GLOBAL";
        this.targetDimensionIds = builder.targetDimensionIds;
        this.activationCondition = builder.activationCondition;
        this.appliesTo = builder.appliesTo;
        this.effectiveFrom = builder.effectiveFrom;
        this.effectiveUntil = builder.effectiveUntil;
        this.exclusionGroup = builder.exclusionGroup;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static CalculationRuleV7 reconstruct(Builder builder) {
        return new CalculationRuleV7(builder);
    }

    public void update(String ruleName, Integer priority, RuleType ruleType,
                       String config, Boolean isEnabled,
                       String scopeType, String targetDimensionIds,
                       String activationCondition, String appliesTo,
                       LocalDate effectiveFrom, LocalDate effectiveUntil,
                       String exclusionGroup) {
        this.ruleName = ruleName;
        this.priority = priority;
        this.ruleType = ruleType;
        this.config = config;
        this.isEnabled = isEnabled;
        this.scopeType = scopeType != null ? scopeType : "GLOBAL";
        this.targetDimensionIds = targetDimensionIds;
        this.activationCondition = activationCondition;
        this.appliesTo = appliesTo;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.exclusionGroup = exclusionGroup;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
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

    public Long getScoringProfileId() {
        return scoringProfileId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public Integer getPriority() {
        return priority;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public String getConfig() {
        return config;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public String getScopeType() {
        return scopeType;
    }

    public String getTargetDimensionIds() {
        return targetDimensionIds;
    }

    public String getActivationCondition() {
        return activationCondition;
    }

    public String getAppliesTo() {
        return appliesTo;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveUntil() {
        return effectiveUntil;
    }

    public String getExclusionGroup() {
        return exclusionGroup;
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
        private Long tenantId;
        private Long scoringProfileId;
        private String ruleCode;
        private String ruleName;
        private Integer priority;
        private RuleType ruleType;
        private String config;
        private Boolean isEnabled;
        private String scopeType;
        private String targetDimensionIds;
        private String activationCondition;
        private String appliesTo;
        private LocalDate effectiveFrom;
        private LocalDate effectiveUntil;
        private String exclusionGroup;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder scoringProfileId(Long scoringProfileId) { this.scoringProfileId = scoringProfileId; return this; }
        public Builder ruleCode(String ruleCode) { this.ruleCode = ruleCode; return this; }
        public Builder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public Builder priority(Integer priority) { this.priority = priority; return this; }
        public Builder ruleType(RuleType ruleType) { this.ruleType = ruleType; return this; }
        public Builder config(String config) { this.config = config; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder scopeType(String scopeType) { this.scopeType = scopeType; return this; }
        public Builder targetDimensionIds(String targetDimensionIds) { this.targetDimensionIds = targetDimensionIds; return this; }
        public Builder activationCondition(String activationCondition) { this.activationCondition = activationCondition; return this; }
        public Builder appliesTo(String appliesTo) { this.appliesTo = appliesTo; return this; }
        public Builder effectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; return this; }
        public Builder effectiveUntil(LocalDate effectiveUntil) { this.effectiveUntil = effectiveUntil; return this; }
        public Builder exclusionGroup(String exclusionGroup) { this.exclusionGroup = exclusionGroup; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public CalculationRuleV7 build() {
            return new CalculationRuleV7(this);
        }
    }
}
