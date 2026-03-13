package com.school.management.domain.inspection.model.v7.template;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * V7 模板分区
 */
public class TemplateSection implements Entity<Long> {

    private Long id;
    private Long templateId;
    private String sectionCode;
    private String sectionName;
    private Integer sortOrder;
    private Integer weight;           // 0-100
    private Boolean isRepeatable;
    private String conditionLogic;    // JSON
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    protected TemplateSection() {
    }

    private TemplateSection(Builder builder) {
        this.id = builder.id;
        this.templateId = builder.templateId;
        this.sectionCode = builder.sectionCode;
        this.sectionName = builder.sectionName;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.weight = builder.weight != null ? builder.weight : 100;
        this.isRepeatable = builder.isRepeatable != null ? builder.isRepeatable : false;
        this.conditionLogic = builder.conditionLogic;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    public static TemplateSection create(Long templateId, String sectionCode, String sectionName, Long createdBy) {
        return builder()
                .templateId(templateId)
                .sectionCode(sectionCode)
                .sectionName(sectionName)
                .createdBy(createdBy)
                .build();
    }

    public static TemplateSection reconstruct(Builder builder) {
        return new TemplateSection(builder);
    }

    public void update(String sectionName, Integer weight,
                       Boolean isRepeatable, String conditionLogic, Long updatedBy) {
        this.sectionName = sectionName;
        this.weight = weight;
        this.isRepeatable = isRepeatable;
        this.conditionLogic = conditionLogic;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void reorder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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

    public Long getTemplateId() {
        return templateId;
    }

    public String getSectionCode() {
        return sectionCode;
    }

    public String getSectionName() {
        return sectionName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Integer getWeight() {
        return weight;
    }

    public Boolean getIsRepeatable() {
        return isRepeatable;
    }

    public String getConditionLogic() {
        return conditionLogic;
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
        private Long templateId;
        private String sectionCode;
        private String sectionName;
        private Integer sortOrder;
        private Integer weight;
        private Boolean isRepeatable;
        private String conditionLogic;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder templateId(Long templateId) { this.templateId = templateId; return this; }
        public Builder sectionCode(String sectionCode) { this.sectionCode = sectionCode; return this; }
        public Builder sectionName(String sectionName) { this.sectionName = sectionName; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder weight(Integer weight) { this.weight = weight; return this; }
        public Builder isRepeatable(Boolean isRepeatable) { this.isRepeatable = isRepeatable; return this; }
        public Builder conditionLogic(String conditionLogic) { this.conditionLogic = conditionLogic; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public TemplateSection build() {
            return new TemplateSection(this);
        }
    }
}
