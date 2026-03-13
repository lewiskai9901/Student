package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V7 等级区间
 */
public class GradeBand implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long scoringProfileId;
    private Long dimensionId;
    private String gradeCode;
    private String gradeName;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private String color;
    private String icon;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected GradeBand() {
    }

    private GradeBand(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.scoringProfileId = builder.scoringProfileId;
        this.dimensionId = builder.dimensionId;
        this.gradeCode = builder.gradeCode;
        this.gradeName = builder.gradeName;
        this.minScore = builder.minScore;
        this.maxScore = builder.maxScore;
        this.color = builder.color;
        this.icon = builder.icon;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static GradeBand reconstruct(Builder builder) {
        return new GradeBand(builder);
    }

    public void update(String gradeName, BigDecimal minScore, BigDecimal maxScore,
                       String color, String icon) {
        this.gradeName = gradeName;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.color = color;
        this.icon = icon;
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

    public Long getDimensionId() {
        return dimensionId;
    }

    public String getGradeCode() {
        return gradeCode;
    }

    public String getGradeName() {
        return gradeName;
    }

    public BigDecimal getMinScore() {
        return minScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public String getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
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
        private Long dimensionId;
        private String gradeCode;
        private String gradeName;
        private BigDecimal minScore;
        private BigDecimal maxScore;
        private String color;
        private String icon;
        private Integer sortOrder;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder scoringProfileId(Long scoringProfileId) { this.scoringProfileId = scoringProfileId; return this; }
        public Builder dimensionId(Long dimensionId) { this.dimensionId = dimensionId; return this; }
        public Builder gradeCode(String gradeCode) { this.gradeCode = gradeCode; return this; }
        public Builder gradeName(String gradeName) { this.gradeName = gradeName; return this; }
        public Builder minScore(BigDecimal minScore) { this.minScore = minScore; return this; }
        public Builder maxScore(BigDecimal maxScore) { this.maxScore = maxScore; return this; }
        public Builder color(String color) { this.color = color; return this; }
        public Builder icon(String icon) { this.icon = icon; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public GradeBand build() {
            return new GradeBand(this);
        }
    }
}
