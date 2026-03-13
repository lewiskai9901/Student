package com.school.management.domain.inspection.model.v7.corrective;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * 问题分类聚合根（树形字典）
 */
public class IssueCategory extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Boolean isEnabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    protected IssueCategory() {
    }

    private IssueCategory(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.parentId = builder.parentId;
        this.categoryCode = builder.categoryCode;
        this.categoryName = builder.categoryName;
        this.description = builder.description;
        this.icon = builder.icon;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    public static IssueCategory create(String categoryCode, String categoryName, Long createdBy) {
        return builder()
                .categoryCode(categoryCode)
                .categoryName(categoryName)
                .createdBy(createdBy)
                .build();
    }

    public static IssueCategory reconstruct(Builder builder) {
        return new IssueCategory(builder);
    }

    public void update(String categoryName, String description, String icon,
                       Integer sortOrder, Boolean isEnabled, Long updatedBy) {
        this.categoryName = categoryName;
        this.description = description;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.isEnabled = isEnabled;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getParentId() { return parentId; }
    public String getCategoryCode() { return categoryCode; }
    public String getCategoryName() { return categoryName; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
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
        private Long parentId;
        private String categoryCode;
        private String categoryName;
        private String description;
        private String icon;
        private Integer sortOrder;
        private Boolean isEnabled;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder parentId(Long parentId) { this.parentId = parentId; return this; }
        public Builder categoryCode(String categoryCode) { this.categoryCode = categoryCode; return this; }
        public Builder categoryName(String categoryName) { this.categoryName = categoryName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder icon(String icon) { this.icon = icon; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public IssueCategory build() { return new IssueCategory(this); }
    }
}
