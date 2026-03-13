package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 重复违规递增策略
 */
public class EscalationPolicy implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long profileId;
    private String policyName;
    private Integer lookupPeriodDays;
    private String escalationMode;      // MULTIPLY, ADD, FIXED_TABLE
    private BigDecimal multiplier;
    private BigDecimal adder;
    private String fixedTable;          // JSON
    private BigDecimal maxEscalationFactor;
    private String matchBy;             // ITEM_CODE, DIMENSION, SECTION, CATEGORY
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected EscalationPolicy() {}

    private EscalationPolicy(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.profileId = builder.profileId;
        this.policyName = builder.policyName;
        this.lookupPeriodDays = builder.lookupPeriodDays != null ? builder.lookupPeriodDays : 30;
        this.escalationMode = builder.escalationMode != null ? builder.escalationMode : "MULTIPLY";
        this.multiplier = builder.multiplier != null ? builder.multiplier : new BigDecimal("2.0");
        this.adder = builder.adder;
        this.fixedTable = builder.fixedTable;
        this.maxEscalationFactor = builder.maxEscalationFactor != null ? builder.maxEscalationFactor : new BigDecimal("5.0");
        this.matchBy = builder.matchBy != null ? builder.matchBy : "ITEM_CODE";
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static EscalationPolicy reconstruct(Builder builder) {
        return new EscalationPolicy(builder);
    }

    public void update(String policyName, Integer lookupPeriodDays, String escalationMode,
                       BigDecimal multiplier, BigDecimal adder, String fixedTable,
                       BigDecimal maxEscalationFactor, String matchBy, Boolean isEnabled) {
        this.policyName = policyName;
        this.lookupPeriodDays = lookupPeriodDays;
        this.escalationMode = escalationMode;
        this.multiplier = multiplier;
        this.adder = adder;
        this.fixedTable = fixedTable;
        this.maxEscalationFactor = maxEscalationFactor;
        this.matchBy = matchBy;
        this.isEnabled = isEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public Long getProfileId() { return profileId; }
    public String getPolicyName() { return policyName; }
    public Integer getLookupPeriodDays() { return lookupPeriodDays; }
    public String getEscalationMode() { return escalationMode; }
    public BigDecimal getMultiplier() { return multiplier; }
    public BigDecimal getAdder() { return adder; }
    public String getFixedTable() { return fixedTable; }
    public BigDecimal getMaxEscalationFactor() { return maxEscalationFactor; }
    public String getMatchBy() { return matchBy; }
    public Boolean getIsEnabled() { return isEnabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long profileId;
        private String policyName;
        private Integer lookupPeriodDays;
        private String escalationMode;
        private BigDecimal multiplier;
        private BigDecimal adder;
        private String fixedTable;
        private BigDecimal maxEscalationFactor;
        private String matchBy;
        private Boolean isEnabled;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder profileId(Long profileId) { this.profileId = profileId; return this; }
        public Builder policyName(String policyName) { this.policyName = policyName; return this; }
        public Builder lookupPeriodDays(Integer lookupPeriodDays) { this.lookupPeriodDays = lookupPeriodDays; return this; }
        public Builder escalationMode(String escalationMode) { this.escalationMode = escalationMode; return this; }
        public Builder multiplier(BigDecimal multiplier) { this.multiplier = multiplier; return this; }
        public Builder adder(BigDecimal adder) { this.adder = adder; return this; }
        public Builder fixedTable(String fixedTable) { this.fixedTable = fixedTable; return this; }
        public Builder maxEscalationFactor(BigDecimal maxEscalationFactor) { this.maxEscalationFactor = maxEscalationFactor; return this; }
        public Builder matchBy(String matchBy) { this.matchBy = matchBy; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public EscalationPolicy build() { return new EscalationPolicy(this); }
    }
}
