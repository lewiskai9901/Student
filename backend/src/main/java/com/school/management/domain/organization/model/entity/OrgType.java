package com.school.management.domain.organization.model.entity;

import com.school.management.domain.shared.ConfigurableType;

import java.util.*;

/**
 * 组织类型领域实体
 * 统一类型系统 Phase 1：支持 category + features + metadataSchema
 */
public class OrgType implements ConfigurableType {

    private Long id;
    private String typeCode;
    private String typeName;
    private String category;          // OrgCategory 枚举值
    private String parentTypeCode;
    private String icon;
    private String description;

    // ========== 行为特征 (JSON) ==========
    private Map<String, Boolean> features;

    // ========== 动态属性定义 (JSON) ==========
    private String metadataSchema;

    // ========== 层级约束 ==========
    private List<String> allowedChildTypeCodes;
    private Integer maxDepth;

    // ========== 跨领域关联 ==========
    private List<String> defaultUserTypeCodes;
    private List<String> defaultPlaceTypeCodes;

    // ========== 系统字段 ==========
    private boolean isSystem;
    private boolean isEnabled;
    private Integer sortOrder;

    public OrgType() {}

    // ========== Getters ==========
    @Override
    public Long getId() { return id; }
    public String getTypeCode() { return typeCode; }
    public String getTypeName() { return typeName; }
    public String getCategory() { return category; }
    public String getParentTypeCode() { return parentTypeCode; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }
    public Map<String, Boolean> getFeatures() { return features; }
    public String getMetadataSchema() { return metadataSchema; }
    public List<String> getAllowedChildTypeCodes() { return allowedChildTypeCodes; }
    public Integer getMaxDepth() { return maxDepth; }
    public List<String> getDefaultUserTypeCodes() { return defaultUserTypeCodes; }
    public List<String> getDefaultPlaceTypeCodes() { return defaultPlaceTypeCodes; }
    public boolean isSystem() { return isSystem; }
    public boolean isEnabled() { return isEnabled; }
    public Integer getSortOrder() { return sortOrder; }

    // ========== 业务方法 ==========

    /**
     * 是否可被检查
     */
    public boolean isInspectionTarget() {
        return hasFeature("inspectionTarget");
    }

    /**
     * 是否数据权限边界
     */
    public boolean isDataPermissionBoundary() {
        return hasFeature("dataPermissionBoundary");
    }

    /**
     * 是否管理成员
     */
    public boolean isMemberManagement() {
        return hasFeature("memberManagement");
    }

    /**
     * 是否允许某 typeCode 作为子类型
     */
    public boolean allowsChildType(String childTypeCode) {
        return allowedChildTypeCodes != null && allowedChildTypeCodes.contains(childTypeCode);
    }

    /**
     * 更新基本信息
     */
    public void update(String typeName, String description, String icon) {
        this.typeName = typeName;
        this.description = description;
        this.icon = icon;
    }

    /**
     * 更新 category 和 features
     */
    public void updateCategoryAndFeatures(String category, Map<String, Boolean> features) {
        this.category = category;
        this.features = features;
    }

    /**
     * 更新 metadataSchema
     */
    public void updateMetadataSchema(String metadataSchema) {
        this.metadataSchema = metadataSchema;
    }

    /**
     * 更新层级约束
     */
    public void updateHierarchyConfig(List<String> allowedChildTypeCodes, Integer maxDepth) {
        this.allowedChildTypeCodes = allowedChildTypeCodes;
        this.maxDepth = maxDepth;
    }

    /**
     * 更新跨领域关联
     */
    public void updateCrossReferences(List<String> defaultUserTypeCodes, List<String> defaultPlaceTypeCodes) {
        this.defaultUserTypeCodes = defaultUserTypeCodes;
        this.defaultPlaceTypeCodes = defaultPlaceTypeCodes;
    }

    /**
     * 更新排序号
     */
    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 设置父类型编码（由 allowedChildTypeCodes 同步自动调用）
     */
    public void setParentTypeCode(String parentTypeCode) {
        this.parentTypeCode = parentTypeCode;
    }

    public void enable() { this.isEnabled = true; }
    public void disable() { this.isEnabled = false; }

    // ========== Builder ==========

    public static OrgTypeBuilder builder() { return new OrgTypeBuilder(); }

    public static class OrgTypeBuilder {
        private Long id;
        private String typeCode;
        private String typeName;
        private String category;
        private String parentTypeCode;
        private String icon;
        private String description;
        private Map<String, Boolean> features;
        private String metadataSchema;
        private List<String> allowedChildTypeCodes;
        private Integer maxDepth;
        private List<String> defaultUserTypeCodes;
        private List<String> defaultPlaceTypeCodes;
        private boolean isSystem;
        private boolean isEnabled;
        private Integer sortOrder;

        public OrgTypeBuilder id(Long id) { this.id = id; return this; }
        public OrgTypeBuilder typeCode(String typeCode) { this.typeCode = typeCode; return this; }
        public OrgTypeBuilder typeName(String typeName) { this.typeName = typeName; return this; }
        public OrgTypeBuilder category(String category) { this.category = category; return this; }
        public OrgTypeBuilder parentTypeCode(String parentTypeCode) { this.parentTypeCode = parentTypeCode; return this; }
        public OrgTypeBuilder icon(String icon) { this.icon = icon; return this; }
        public OrgTypeBuilder description(String description) { this.description = description; return this; }
        public OrgTypeBuilder features(Map<String, Boolean> features) { this.features = features; return this; }
        public OrgTypeBuilder metadataSchema(String metadataSchema) { this.metadataSchema = metadataSchema; return this; }
        public OrgTypeBuilder allowedChildTypeCodes(List<String> allowedChildTypeCodes) { this.allowedChildTypeCodes = allowedChildTypeCodes; return this; }
        public OrgTypeBuilder maxDepth(Integer maxDepth) { this.maxDepth = maxDepth; return this; }
        public OrgTypeBuilder defaultUserTypeCodes(List<String> defaultUserTypeCodes) { this.defaultUserTypeCodes = defaultUserTypeCodes; return this; }
        public OrgTypeBuilder defaultPlaceTypeCodes(List<String> defaultPlaceTypeCodes) { this.defaultPlaceTypeCodes = defaultPlaceTypeCodes; return this; }
        public OrgTypeBuilder isSystem(boolean isSystem) { this.isSystem = isSystem; return this; }
        public OrgTypeBuilder isEnabled(boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public OrgTypeBuilder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }

        public OrgType build() {
            OrgType type = new OrgType();
            type.id = this.id;
            type.typeCode = this.typeCode;
            type.typeName = this.typeName;
            type.category = this.category;
            type.parentTypeCode = this.parentTypeCode;
            type.icon = this.icon;
            type.description = this.description;
            type.features = this.features;
            type.metadataSchema = this.metadataSchema;
            type.allowedChildTypeCodes = this.allowedChildTypeCodes;
            type.maxDepth = this.maxDepth;
            type.defaultUserTypeCodes = this.defaultUserTypeCodes;
            type.defaultPlaceTypeCodes = this.defaultPlaceTypeCodes;
            type.isSystem = this.isSystem;
            type.isEnabled = this.isEnabled;
            type.sortOrder = this.sortOrder;
            return type;
        }
    }
}
