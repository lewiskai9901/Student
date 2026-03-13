package com.school.management.domain.inspection.model.v7.compliance;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * 检查项与合规条款的映射实体
 */
public class ItemComplianceMapping extends AggregateRoot<Long> {

    private Long id;
    private Long templateItemId;
    private Long clauseId;
    private String coverageLevel;
    private String notes;
    private LocalDateTime createdAt;

    protected ItemComplianceMapping() {
    }

    private ItemComplianceMapping(Builder builder) {
        this.id = builder.id;
        this.templateItemId = builder.templateItemId;
        this.clauseId = builder.clauseId;
        this.coverageLevel = builder.coverageLevel;
        this.notes = builder.notes;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    public static ItemComplianceMapping create(Long itemId, Long clauseId, String coverageLevel) {
        return builder()
                .templateItemId(itemId)
                .clauseId(clauseId)
                .coverageLevel(coverageLevel)
                .build();
    }

    public static ItemComplianceMapping reconstruct(Builder builder) {
        return new ItemComplianceMapping(builder);
    }

    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTemplateItemId() { return templateItemId; }
    public Long getClauseId() { return clauseId; }
    public String getCoverageLevel() { return coverageLevel; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long templateItemId;
        private Long clauseId;
        private String coverageLevel;
        private String notes;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder templateItemId(Long templateItemId) { this.templateItemId = templateItemId; return this; }
        public Builder clauseId(Long clauseId) { this.clauseId = clauseId; return this; }
        public Builder coverageLevel(String coverageLevel) { this.coverageLevel = coverageLevel; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ItemComplianceMapping build() { return new ItemComplianceMapping(this); }
    }
}
