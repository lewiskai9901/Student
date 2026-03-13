package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V7 评分维度
 */
public class ScoreDimension implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long scoringProfileId;
    private String dimensionCode;
    private String dimensionName;
    private Integer weight;
    private BigDecimal baseScore;
    private BigDecimal passThreshold;
    private String sourceType;       // SECTION or MODULE
    private Long moduleTemplateId;   // non-null when sourceType=MODULE
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ScoreDimension() {
    }

    private ScoreDimension(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.scoringProfileId = builder.scoringProfileId;
        this.dimensionCode = builder.dimensionCode;
        this.dimensionName = builder.dimensionName;
        this.weight = builder.weight != null ? builder.weight : 100;
        this.baseScore = builder.baseScore != null ? builder.baseScore : BigDecimal.valueOf(100);
        this.passThreshold = builder.passThreshold;
        this.sourceType = builder.sourceType != null ? builder.sourceType : "SECTION";
        this.moduleTemplateId = builder.moduleTemplateId;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static ScoreDimension reconstruct(Builder builder) {
        return new ScoreDimension(builder);
    }

    public void update(String dimensionName, Integer weight, BigDecimal baseScore,
                       BigDecimal passThreshold) {
        this.dimensionName = dimensionName;
        this.weight = weight;
        this.baseScore = baseScore;
        this.passThreshold = passThreshold;
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

    public String getDimensionCode() {
        return dimensionCode;
    }

    public String getDimensionName() {
        return dimensionName;
    }

    public Integer getWeight() {
        return weight;
    }

    public BigDecimal getBaseScore() {
        return baseScore;
    }

    public BigDecimal getPassThreshold() {
        return passThreshold;
    }

    public String getSourceType() {
        return sourceType;
    }

    public Long getModuleTemplateId() {
        return moduleTemplateId;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long scoringProfileId;
        private String dimensionCode;
        private String dimensionName;
        private Integer weight;
        private BigDecimal baseScore;
        private BigDecimal passThreshold;
        private String sourceType;
        private Long moduleTemplateId;
        private Integer sortOrder;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder scoringProfileId(Long scoringProfileId) { this.scoringProfileId = scoringProfileId; return this; }
        public Builder dimensionCode(String dimensionCode) { this.dimensionCode = dimensionCode; return this; }
        public Builder dimensionName(String dimensionName) { this.dimensionName = dimensionName; return this; }
        public Builder weight(Integer weight) { this.weight = weight; return this; }
        public Builder baseScore(BigDecimal baseScore) { this.baseScore = baseScore; return this; }
        public Builder passThreshold(BigDecimal passThreshold) { this.passThreshold = passThreshold; return this; }
        public Builder sourceType(String sourceType) { this.sourceType = sourceType; return this; }
        public Builder moduleTemplateId(Long moduleTemplateId) { this.moduleTemplateId = moduleTemplateId; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ScoreDimension build() {
            return new ScoreDimension(this);
        }
    }
}
