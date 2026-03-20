package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * 评级维度 — 同一份检查数据可评出多个奖项
 */
public class RatingDimension extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private String dimensionName;
    private String sectionIds;         // JSON: 关联的分区ID列表
    private String aggregation;        // WEIGHTED_AVG / SUM / AVG / MAX / MIN
    private String gradeBands;         // JSON: [{code,name,minScore,maxScore,color}]
    private String awardName;
    private Boolean rankingEnabled;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected RatingDimension() {
    }

    private RatingDimension(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.projectId = builder.projectId;
        this.dimensionName = builder.dimensionName;
        this.sectionIds = builder.sectionIds;
        this.aggregation = builder.aggregation != null ? builder.aggregation : "WEIGHTED_AVG";
        this.gradeBands = builder.gradeBands;
        this.awardName = builder.awardName;
        this.rankingEnabled = builder.rankingEnabled != null ? builder.rankingEnabled : true;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static RatingDimension create(Long projectId, String dimensionName,
                                          String sectionIds, Long createdBy) {
        return builder()
                .projectId(projectId)
                .dimensionName(dimensionName)
                .sectionIds(sectionIds)
                .createdBy(createdBy)
                .build();
    }

    public static RatingDimension reconstruct(Builder builder) {
        return new RatingDimension(builder);
    }

    public void update(String dimensionName, String sectionIds, String aggregation,
                       String gradeBands, String awardName, Boolean rankingEnabled) {
        this.dimensionName = dimensionName;
        this.sectionIds = sectionIds;
        this.aggregation = aggregation;
        this.gradeBands = gradeBands;
        this.awardName = awardName;
        this.rankingEnabled = rankingEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getProjectId() { return projectId; }
    public String getDimensionName() { return dimensionName; }
    public String getSectionIds() { return sectionIds; }
    public String getAggregation() { return aggregation; }
    public String getGradeBands() { return gradeBands; }
    public String getAwardName() { return awardName; }
    public Boolean getRankingEnabled() { return rankingEnabled; }
    public Integer getSortOrder() { return sortOrder; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long projectId;
        private String dimensionName;
        private String sectionIds;
        private String aggregation;
        private String gradeBands;
        private String awardName;
        private Boolean rankingEnabled;
        private Integer sortOrder;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder dimensionName(String dimensionName) { this.dimensionName = dimensionName; return this; }
        public Builder sectionIds(String sectionIds) { this.sectionIds = sectionIds; return this; }
        public Builder aggregation(String aggregation) { this.aggregation = aggregation; return this; }
        public Builder gradeBands(String gradeBands) { this.gradeBands = gradeBands; return this; }
        public Builder awardName(String awardName) { this.awardName = awardName; return this; }
        public Builder rankingEnabled(Boolean rankingEnabled) { this.rankingEnabled = rankingEnabled; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public RatingDimension build() { return new RatingDimension(this); }
    }
}
