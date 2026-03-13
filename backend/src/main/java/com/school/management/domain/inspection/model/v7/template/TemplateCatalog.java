package com.school.management.domain.inspection.model.v7.template;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * V7 模板分类目录（树形结构）
 */
public class TemplateCatalog extends AggregateRoot<Long> {

    private Long id;
    private Long parentId;
    private String catalogCode;
    private String catalogName;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Boolean isEnabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    protected TemplateCatalog() {
    }

    private TemplateCatalog(Builder builder) {
        this.id = builder.id;
        this.parentId = builder.parentId;
        this.catalogCode = builder.catalogCode;
        this.catalogName = builder.catalogName;
        this.description = builder.description;
        this.icon = builder.icon;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    public static TemplateCatalog create(String catalogCode, String catalogName, Long createdBy) {
        return builder()
                .catalogCode(catalogCode)
                .catalogName(catalogName)
                .createdBy(createdBy)
                .build();
    }

    public static TemplateCatalog reconstruct(Builder builder) {
        return new TemplateCatalog(builder);
    }

    public void update(String catalogName, String description, Long parentId,
                       String icon, Integer sortOrder, Boolean isEnabled, Long updatedBy) {
        this.catalogName = catalogName;
        this.description = description;
        this.parentId = parentId;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.isEnabled = isEnabled;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getCatalogCode() {
        return catalogCode;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long parentId;
        private String catalogCode;
        private String catalogName;
        private String description;
        private String icon;
        private Integer sortOrder;
        private Boolean isEnabled;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder parentId(Long parentId) { this.parentId = parentId; return this; }
        public Builder catalogCode(String catalogCode) { this.catalogCode = catalogCode; return this; }
        public Builder catalogName(String catalogName) { this.catalogName = catalogName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder icon(String icon) { this.icon = icon; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public TemplateCatalog build() {
            return new TemplateCatalog(this);
        }
    }
}
