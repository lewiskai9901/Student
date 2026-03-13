package com.school.management.domain.inspection.model.v7.execution;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * V7 评分预设 - 快速评分功能
 * 允许检查员保存常用评分组合，一键应用到模板的所有检查项
 */
public class ScoringPreset implements Entity<Long> {

    private Long id;
    private Long templateId;
    private String presetName;
    private String presetType; // FULL_PASS / FULL_FAIL / CUSTOM
    private String itemValues; // JSON: [{itemId, score, comment}]
    private Integer usageCount;
    private Long createdBy;
    private LocalDateTime createdAt;

    protected ScoringPreset() {
    }

    private ScoringPreset(Builder builder) {
        this.id = builder.id;
        this.templateId = builder.templateId;
        this.presetName = builder.presetName;
        this.presetType = builder.presetType;
        this.itemValues = builder.itemValues;
        this.usageCount = builder.usageCount != null ? builder.usageCount : 0;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    public static ScoringPreset create(Long templateId, String presetName, String presetType,
                                        String itemValues, Long createdBy) {
        if (templateId == null) {
            throw new IllegalArgumentException("templateId不能为空");
        }
        if (presetName == null || presetName.isBlank()) {
            throw new IllegalArgumentException("预设名称不能为空");
        }
        if (presetType == null || presetType.isBlank()) {
            throw new IllegalArgumentException("预设类型不能为空");
        }
        return builder()
                .templateId(templateId)
                .presetName(presetName)
                .presetType(presetType)
                .itemValues(itemValues)
                .createdBy(createdBy)
                .build();
    }

    public static ScoringPreset reconstruct(Builder builder) {
        return new ScoringPreset(builder);
    }

    public void incrementUsageCount() {
        this.usageCount = (this.usageCount != null ? this.usageCount : 0) + 1;
    }

    public void updateItemValues(String itemValues) {
        this.itemValues = itemValues;
    }

    public void updateName(String presetName) {
        if (presetName == null || presetName.isBlank()) {
            throw new IllegalArgumentException("预设名称不能为空");
        }
        this.presetName = presetName;
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getTemplateId() { return templateId; }
    public String getPresetName() { return presetName; }
    public String getPresetType() { return presetType; }
    public String getItemValues() { return itemValues; }
    public Integer getUsageCount() { return usageCount; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long templateId;
        private String presetName;
        private String presetType;
        private String itemValues;
        private Integer usageCount;
        private Long createdBy;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder templateId(Long templateId) { this.templateId = templateId; return this; }
        public Builder presetName(String presetName) { this.presetName = presetName; return this; }
        public Builder presetType(String presetType) { this.presetType = presetType; return this; }
        public Builder itemValues(String itemValues) { this.itemValues = itemValues; return this; }
        public Builder usageCount(Integer usageCount) { this.usageCount = usageCount; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ScoringPreset build() { return new ScoringPreset(this); }
    }
}
