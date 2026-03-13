package com.school.management.domain.inspection.model.v7.template;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * V7 模板模块引用关系
 * 组合模板通过此实体引用子模块模板
 */
public class TemplateModuleRef implements Entity<Long> {

    private Long id;
    private Long compositeTemplateId;
    private Long moduleTemplateId;
    private Integer sortOrder;
    private Integer weight;
    private String overrideConfig;  // JSON
    private LocalDateTime createdAt;

    protected TemplateModuleRef() {
    }

    private TemplateModuleRef(Builder builder) {
        this.id = builder.id;
        this.compositeTemplateId = builder.compositeTemplateId;
        this.moduleTemplateId = builder.moduleTemplateId;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.weight = builder.weight != null ? builder.weight : 100;
        this.overrideConfig = builder.overrideConfig;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    public static TemplateModuleRef create(Long compositeTemplateId, Long moduleTemplateId,
                                              Integer sortOrder, Integer weight) {
        return builder()
                .compositeTemplateId(compositeTemplateId)
                .moduleTemplateId(moduleTemplateId)
                .sortOrder(sortOrder)
                .weight(weight)
                .build();
    }

    public static TemplateModuleRef reconstruct(Builder builder) {
        return new TemplateModuleRef(builder);
    }

    public void reorder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void updateWeight(Integer weight) {
        this.weight = weight != null ? weight : 100;
    }

    // Getters
    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompositeTemplateId() { return compositeTemplateId; }
    public Long getModuleTemplateId() { return moduleTemplateId; }
    public Integer getSortOrder() { return sortOrder; }
    public Integer getWeight() { return weight; }
    public String getOverrideConfig() { return overrideConfig; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long compositeTemplateId;
        private Long moduleTemplateId;
        private Integer sortOrder;
        private Integer weight;
        private String overrideConfig;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder compositeTemplateId(Long compositeTemplateId) { this.compositeTemplateId = compositeTemplateId; return this; }
        public Builder moduleTemplateId(Long moduleTemplateId) { this.moduleTemplateId = moduleTemplateId; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder weight(Integer weight) { this.weight = weight; return this; }
        public Builder overrideConfig(String overrideConfig) { this.overrideConfig = overrideConfig; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public TemplateModuleRef build() { return new TemplateModuleRef(this); }
    }
}
