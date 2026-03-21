package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * 评分方案即时规则实体
 * ruleType: VETO/PENALTY/BONUS/PROGRESSIVE/CUSTOM
 */
public class PolicyCalcRule implements Entity<Long> {

    private Long id;
    private Long policyId;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private Integer priority;
    private String config;  // JSON: 规则参数
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected PolicyCalcRule() {
    }

    private PolicyCalcRule(Builder builder) {
        this.id = builder.id;
        this.policyId = builder.policyId;
        this.ruleCode = builder.ruleCode;
        this.ruleName = builder.ruleName;
        this.ruleType = builder.ruleType;
        this.priority = builder.priority != null ? builder.priority : 0;
        this.config = builder.config;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static PolicyCalcRule reconstruct(Builder builder) {
        return new PolicyCalcRule(builder);
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }
    public Long getPolicyId() { return policyId; }
    public String getRuleCode() { return ruleCode; }
    public String getRuleName() { return ruleName; }
    public String getRuleType() { return ruleType; }
    public Integer getPriority() { return priority; }
    public String getConfig() { return config; }
    public Boolean getIsEnabled() { return isEnabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long policyId;
        private String ruleCode;
        private String ruleName;
        private String ruleType;
        private Integer priority;
        private String config;
        private Boolean isEnabled;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder policyId(Long policyId) { this.policyId = policyId; return this; }
        public Builder ruleCode(String ruleCode) { this.ruleCode = ruleCode; return this; }
        public Builder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public Builder ruleType(String ruleType) { this.ruleType = ruleType; return this; }
        public Builder priority(Integer priority) { this.priority = priority; return this; }
        public Builder config(String config) { this.config = config; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public PolicyCalcRule build() { return new PolicyCalcRule(this); }
    }
}
