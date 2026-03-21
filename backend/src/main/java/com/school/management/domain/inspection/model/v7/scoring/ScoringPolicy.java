package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * 评分方案聚合根 — 可跨模板复用的独立评分配置
 */
public class ScoringPolicy extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private String policyCode;
    private String policyName;
    private String description;
    private Integer precisionDigits;
    private Boolean isSystem;
    private Boolean isEnabled;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    protected ScoringPolicy() {
    }

    private ScoringPolicy(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.policyCode = builder.policyCode;
        this.policyName = builder.policyName;
        this.description = builder.description;
        this.precisionDigits = builder.precisionDigits != null ? builder.precisionDigits : 2;
        this.isSystem = builder.isSystem != null ? builder.isSystem : false;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    public static ScoringPolicy create(String policyCode, String policyName, Long createdBy) {
        return builder()
                .policyCode(policyCode)
                .policyName(policyName)
                .createdBy(createdBy)
                .build();
    }

    public static ScoringPolicy reconstruct(Builder builder) {
        return new ScoringPolicy(builder);
    }

    public void update(String policyName, String description,
                       Integer precisionDigits, Integer sortOrder, Long updatedBy) {
        if (Boolean.TRUE.equals(this.isSystem)) {
            throw new IllegalStateException("系统预置评分方案不允许修改");
        }
        this.policyName = policyName;
        this.description = description;
        this.precisionDigits = precisionDigits;
        this.sortOrder = sortOrder;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void enable() {
        this.isEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        if (Boolean.TRUE.equals(this.isSystem)) {
            throw new IllegalStateException("系统预置评分方案不允许禁用");
        }
        this.isEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public String getPolicyCode() { return policyCode; }
    public String getPolicyName() { return policyName; }
    public String getDescription() { return description; }
    public Integer getPrecisionDigits() { return precisionDigits; }
    public Boolean getIsSystem() { return isSystem; }
    public Boolean getIsEnabled() { return isEnabled; }
    public Integer getSortOrder() { return sortOrder; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private String policyCode;
        private String policyName;
        private String description;
        private Integer precisionDigits;
        private Boolean isSystem;
        private Boolean isEnabled;
        private Integer sortOrder;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder policyCode(String policyCode) { this.policyCode = policyCode; return this; }
        public Builder policyName(String policyName) { this.policyName = policyName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder precisionDigits(Integer precisionDigits) { this.precisionDigits = precisionDigits; return this; }
        public Builder isSystem(Boolean isSystem) { this.isSystem = isSystem; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ScoringPolicy build() { return new ScoringPolicy(this); }
    }
}
