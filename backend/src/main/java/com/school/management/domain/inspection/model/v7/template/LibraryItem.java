package com.school.management.domain.inspection.model.v7.template;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * V7 检查项库 - 全局共享检查项
 */
public class LibraryItem extends AggregateRoot<Long> {

    private String itemCode;
    private String itemName;
    private String description;
    private ItemType itemType;
    private String category;
    private String tags;
    private String defaultConfig;
    private String defaultValidationRules;
    private String defaultScoringConfig;
    private String defaultHelpContent;
    private Integer usageCount;
    private Boolean isStandard;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected LibraryItem() {
    }

    private LibraryItem(Builder builder) {
        this.id = builder.id;
        this.itemCode = builder.itemCode;
        this.itemName = builder.itemName;
        this.description = builder.description;
        this.itemType = builder.itemType;
        this.category = builder.category;
        this.tags = builder.tags;
        this.defaultConfig = builder.defaultConfig;
        this.defaultValidationRules = builder.defaultValidationRules;
        this.defaultScoringConfig = builder.defaultScoringConfig;
        this.defaultHelpContent = builder.defaultHelpContent;
        this.usageCount = builder.usageCount != null ? builder.usageCount : 0;
        this.isStandard = builder.isStandard != null ? builder.isStandard : false;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static LibraryItem create(String itemCode, String itemName, ItemType itemType, Long createdBy) {
        return builder()
                .itemCode(itemCode)
                .itemName(itemName)
                .itemType(itemType)
                .createdBy(createdBy)
                .build();
    }

    public static LibraryItem reconstruct(Builder builder) {
        return new LibraryItem(builder);
    }

    public void update(String itemName, String description, ItemType itemType,
                       String category, String tags, String defaultConfig,
                       String defaultValidationRules, String defaultScoringConfig,
                       String defaultHelpContent, Boolean isStandard) {
        this.itemName = itemName;
        this.description = description;
        this.itemType = itemType;
        this.category = category;
        this.tags = tags;
        this.defaultConfig = defaultConfig;
        this.defaultValidationRules = defaultValidationRules;
        this.defaultScoringConfig = defaultScoringConfig;
        this.defaultHelpContent = defaultHelpContent;
        if (isStandard != null) {
            this.isStandard = isStandard;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementUsageCount() {
        this.usageCount = (this.usageCount != null ? this.usageCount : 0) + 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void decrementUsageCount() {
        if (this.usageCount != null && this.usageCount > 0) {
            this.usageCount--;
        }
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public String getDescription() { return description; }
    public ItemType getItemType() { return itemType; }
    public String getCategory() { return category; }
    public String getTags() { return tags; }
    public String getDefaultConfig() { return defaultConfig; }
    public String getDefaultValidationRules() { return defaultValidationRules; }
    public String getDefaultScoringConfig() { return defaultScoringConfig; }
    public String getDefaultHelpContent() { return defaultHelpContent; }
    public Integer getUsageCount() { return usageCount; }
    public Boolean getIsStandard() { return isStandard; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String itemCode;
        private String itemName;
        private String description;
        private ItemType itemType;
        private String category;
        private String tags;
        private String defaultConfig;
        private String defaultValidationRules;
        private String defaultScoringConfig;
        private String defaultHelpContent;
        private Integer usageCount;
        private Boolean isStandard;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder itemCode(String itemCode) { this.itemCode = itemCode; return this; }
        public Builder itemName(String itemName) { this.itemName = itemName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder itemType(ItemType itemType) { this.itemType = itemType; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder tags(String tags) { this.tags = tags; return this; }
        public Builder defaultConfig(String defaultConfig) { this.defaultConfig = defaultConfig; return this; }
        public Builder defaultValidationRules(String defaultValidationRules) { this.defaultValidationRules = defaultValidationRules; return this; }
        public Builder defaultScoringConfig(String defaultScoringConfig) { this.defaultScoringConfig = defaultScoringConfig; return this; }
        public Builder defaultHelpContent(String defaultHelpContent) { this.defaultHelpContent = defaultHelpContent; return this; }
        public Builder usageCount(Integer usageCount) { this.usageCount = usageCount; return this; }
        public Builder isStandard(Boolean isStandard) { this.isStandard = isStandard; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public LibraryItem build() { return new LibraryItem(this); }
    }
}
