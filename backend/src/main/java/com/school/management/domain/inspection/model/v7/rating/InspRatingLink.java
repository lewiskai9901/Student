package com.school.management.domain.inspection.model.v7.rating;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * V7 检查项目 → 评级配置的关联实体
 * 桥接 V7 检查分析与评级系统
 */
public class InspRatingLink implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private Long ratingConfigId;
    private String periodType;
    private boolean autoCalculate;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected InspRatingLink() {}

    public static InspRatingLink create(Long projectId, Long ratingConfigId,
                                         String periodType, boolean autoCalculate,
                                         Long createdBy) {
        InspRatingLink link = new InspRatingLink();
        link.tenantId = 0L;
        link.projectId = projectId;
        link.ratingConfigId = ratingConfigId;
        link.periodType = periodType;
        link.autoCalculate = autoCalculate;
        link.createdBy = createdBy;
        link.createdAt = LocalDateTime.now();
        return link;
    }

    public void update(String periodType, boolean autoCalculate) {
        if (periodType != null) this.periodType = periodType;
        this.autoCalculate = autoCalculate;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters

    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public Long getProjectId() { return projectId; }
    public Long getRatingConfigId() { return ratingConfigId; }
    public String getPeriodType() { return periodType; }
    public boolean isAutoCalculate() { return autoCalculate; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Builder for reconstruction

    public static class Builder {
        private final InspRatingLink l = new InspRatingLink();

        public Builder id(Long v) { l.id = v; return this; }
        public Builder tenantId(Long v) { l.tenantId = v; return this; }
        public Builder projectId(Long v) { l.projectId = v; return this; }
        public Builder ratingConfigId(Long v) { l.ratingConfigId = v; return this; }
        public Builder periodType(String v) { l.periodType = v; return this; }
        public Builder autoCalculate(boolean v) { l.autoCalculate = v; return this; }
        public Builder createdBy(Long v) { l.createdBy = v; return this; }
        public Builder createdAt(LocalDateTime v) { l.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v) { l.updatedAt = v; return this; }
        public InspRatingLink build() { return l; }
    }

    public static Builder builder() { return new Builder(); }
}
