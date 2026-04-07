package com.school.management.domain.inspection.model.v7.template;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V7 模板字段（22种类型的表单字段）
 */
public class TemplateItem implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long sectionId;
    private String itemCode;
    private String itemName;
    private String description;
    private ItemType itemType;
    private String config;            // JSON: 类型特定配置
    private String validationRules;   // JSON: 验证规则数组
    private Long responseSetId;
    private String scoringConfig;     // JSON: 评分+归一化配置
    private Long dimensionId;
    private String helpContent;
    private Boolean isRequired;
    private Boolean isScored;
    private Boolean requireEvidence;
    private BigDecimal itemWeight;    // 项目权重(维度内), 默认1.00
    private Integer sortOrder;
    private String conditionLogic;    // JSON
    private Long libraryItemId;       // 来源库项目ID
    private Boolean syncWithLibrary;  // 是否与库同步
    private String visibilityLogic;   // JSON: 显示条件(条件逻辑V2)
    private String scoringLogic;      // JSON: 计分条件(条件逻辑V2)
    private String inputMode;         // INLINE | EVENT_STREAM
    private String linkedEventTypeCode; // 关联事件类型编码(扣分时触发)
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    protected TemplateItem() {
    }

    private TemplateItem(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.sectionId = builder.sectionId;
        this.itemCode = builder.itemCode;
        this.itemName = builder.itemName;
        this.description = builder.description;
        this.itemType = builder.itemType;
        this.config = builder.config;
        this.validationRules = builder.validationRules;
        this.responseSetId = builder.responseSetId;
        this.scoringConfig = builder.scoringConfig;
        this.dimensionId = builder.dimensionId;
        this.helpContent = builder.helpContent;
        this.isRequired = builder.isRequired != null ? builder.isRequired : false;
        this.isScored = builder.isScored != null ? builder.isScored : false;
        this.requireEvidence = builder.requireEvidence != null ? builder.requireEvidence : false;
        this.itemWeight = builder.itemWeight != null ? builder.itemWeight : BigDecimal.ONE;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.conditionLogic = builder.conditionLogic;
        this.libraryItemId = builder.libraryItemId;
        this.syncWithLibrary = builder.syncWithLibrary != null ? builder.syncWithLibrary : false;
        this.visibilityLogic = builder.visibilityLogic;
        this.scoringLogic = builder.scoringLogic;
        this.inputMode = builder.inputMode;
        this.linkedEventTypeCode = builder.linkedEventTypeCode;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    public static TemplateItem create(Long sectionId, String itemCode, String itemName,
                                      ItemType itemType, Long createdBy) {
        return builder()
                .sectionId(sectionId)
                .itemCode(itemCode)
                .itemName(itemName)
                .itemType(itemType)
                .createdBy(createdBy)
                .build();
    }

    public static TemplateItem reconstruct(Builder builder) {
        return new TemplateItem(builder);
    }

    public void update(String itemName, String description, ItemType itemType,
                       String config, String validationRules, Long responseSetId,
                       String scoringConfig, Long dimensionId, String helpContent,
                       Boolean isRequired, Boolean isScored, Boolean requireEvidence,
                       BigDecimal itemWeight, String conditionLogic, String inputMode,
                       Long updatedBy) {
        if (itemName != null) this.itemName = itemName;
        if (description != null) this.description = description;
        if (itemType != null) this.itemType = itemType;
        if (config != null) this.config = config;
        if (validationRules != null) this.validationRules = validationRules;
        if (responseSetId != null) this.responseSetId = responseSetId;
        if (scoringConfig != null) this.scoringConfig = scoringConfig;
        // dimensionId can be explicitly set to null (remove from dimension)
        this.dimensionId = dimensionId;
        if (helpContent != null) this.helpContent = helpContent;
        if (isRequired != null) this.isRequired = isRequired;
        if (isScored != null) this.isScored = isScored;
        if (requireEvidence != null) this.requireEvidence = requireEvidence;
        if (itemWeight != null) this.itemWeight = itemWeight;
        if (conditionLogic != null) this.conditionLogic = conditionLogic;
        if (inputMode != null) this.inputMode = inputMode;
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

    public Long getTenantId() {
        return tenantId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public String getConfig() {
        return config;
    }

    public String getValidationRules() {
        return validationRules;
    }

    public Long getResponseSetId() {
        return responseSetId;
    }

    public String getScoringConfig() {
        return scoringConfig;
    }

    public Long getDimensionId() {
        return dimensionId;
    }

    public String getHelpContent() {
        return helpContent;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public Boolean getIsScored() {
        return isScored;
    }

    public Boolean getRequireEvidence() {
        return requireEvidence;
    }

    public BigDecimal getItemWeight() {
        return itemWeight;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getConditionLogic() {
        return conditionLogic;
    }

    public Long getLibraryItemId() {
        return libraryItemId;
    }

    public Boolean getSyncWithLibrary() {
        return syncWithLibrary;
    }

    public String getVisibilityLogic() {
        return visibilityLogic;
    }

    public String getScoringLogic() {
        return scoringLogic;
    }

    public String getInputMode() {
        return inputMode;
    }

    public String getLinkedEventTypeCode() {
        return linkedEventTypeCode;
    }

    public void linkToLibrary(Long libraryItemId, boolean syncWithLibrary) {
        this.libraryItemId = libraryItemId;
        this.syncWithLibrary = syncWithLibrary;
        this.updatedAt = LocalDateTime.now();
    }

    public void unlinkFromLibrary() {
        this.libraryItemId = null;
        this.syncWithLibrary = false;
        this.updatedAt = LocalDateTime.now();
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
        private Long tenantId;
        private Long sectionId;
        private String itemCode;
        private String itemName;
        private String description;
        private ItemType itemType;
        private String config;
        private String validationRules;
        private Long responseSetId;
        private String scoringConfig;
        private Long dimensionId;
        private String helpContent;
        private Boolean isRequired;
        private Boolean isScored;
        private Boolean requireEvidence;
        private BigDecimal itemWeight;
        private Integer sortOrder;
        private String conditionLogic;
        private Long libraryItemId;
        private Boolean syncWithLibrary;
        private String visibilityLogic;
        private String scoringLogic;
        private String inputMode;
        private String linkedEventTypeCode;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder sectionId(Long sectionId) { this.sectionId = sectionId; return this; }
        public Builder itemCode(String itemCode) { this.itemCode = itemCode; return this; }
        public Builder itemName(String itemName) { this.itemName = itemName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder itemType(ItemType itemType) { this.itemType = itemType; return this; }
        public Builder config(String config) { this.config = config; return this; }
        public Builder validationRules(String validationRules) { this.validationRules = validationRules; return this; }
        public Builder responseSetId(Long responseSetId) { this.responseSetId = responseSetId; return this; }
        public Builder scoringConfig(String scoringConfig) { this.scoringConfig = scoringConfig; return this; }
        public Builder dimensionId(Long dimensionId) { this.dimensionId = dimensionId; return this; }
        public Builder helpContent(String helpContent) { this.helpContent = helpContent; return this; }
        public Builder isRequired(Boolean isRequired) { this.isRequired = isRequired; return this; }
        public Builder isScored(Boolean isScored) { this.isScored = isScored; return this; }
        public Builder requireEvidence(Boolean requireEvidence) { this.requireEvidence = requireEvidence; return this; }
        public Builder itemWeight(BigDecimal itemWeight) { this.itemWeight = itemWeight; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder conditionLogic(String conditionLogic) { this.conditionLogic = conditionLogic; return this; }
        public Builder libraryItemId(Long libraryItemId) { this.libraryItemId = libraryItemId; return this; }
        public Builder syncWithLibrary(Boolean syncWithLibrary) { this.syncWithLibrary = syncWithLibrary; return this; }
        public Builder visibilityLogic(String visibilityLogic) { this.visibilityLogic = visibilityLogic; return this; }
        public Builder scoringLogic(String scoringLogic) { this.scoringLogic = scoringLogic; return this; }
        public Builder inputMode(String inputMode) { this.inputMode = inputMode; return this; }
        public Builder linkedEventTypeCode(String linkedEventTypeCode) { this.linkedEventTypeCode = linkedEventTypeCode; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public TemplateItem build() {
            return new TemplateItem(this);
        }
    }
}
