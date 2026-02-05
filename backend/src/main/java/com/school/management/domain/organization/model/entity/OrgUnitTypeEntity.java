package com.school.management.domain.organization.model.entity;

import com.school.management.domain.shared.Entity;

/**
 * 组织类型领域实体
 * 定义组织单元的类型和层级配置
 */
public class OrgUnitTypeEntity implements Entity<Long> {

    private Long id;

    /**
     * 类型编码（唯一标识）
     */
    private String typeCode;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 父类型编码
     */
    private String parentTypeCode;

    /**
     * 层级顺序 (1=一级, 2=二级...)
     */
    private Integer levelOrder;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 描述
     */
    private String description;

    // ========== 组织特性 ==========

    /**
     * 是否教学单位（vs 职能部门）
     */
    private boolean isAcademic;

    /**
     * 是否可被检查
     */
    private boolean canBeInspected;

    /**
     * 是否可有子级
     */
    private boolean canHaveChildren;

    /**
     * 最大子级深度
     */
    private Integer maxDepth;

    // ========== 系统字段 ==========

    /**
     * 是否系统预置
     */
    private boolean isSystem;

    /**
     * 是否启用
     */
    private boolean isEnabled;

    /**
     * 排序号
     */
    private Integer sortOrder;

    public OrgUnitTypeEntity() {}

    public OrgUnitTypeEntity(Long id, String typeCode, String typeName, String parentTypeCode,
                             Integer levelOrder, String icon, String color, String description,
                             boolean isAcademic, boolean canBeInspected, boolean canHaveChildren,
                             Integer maxDepth, boolean isSystem, boolean isEnabled, Integer sortOrder) {
        this.id = id;
        this.typeCode = typeCode;
        this.typeName = typeName;
        this.parentTypeCode = parentTypeCode;
        this.levelOrder = levelOrder;
        this.icon = icon;
        this.color = color;
        this.description = description;
        this.isAcademic = isAcademic;
        this.canBeInspected = canBeInspected;
        this.canHaveChildren = canHaveChildren;
        this.maxDepth = maxDepth;
        this.isSystem = isSystem;
        this.isEnabled = isEnabled;
        this.sortOrder = sortOrder;
    }

    // Getters
    @Override
    public Long getId() { return id; }
    public String getTypeCode() { return typeCode; }
    public String getTypeName() { return typeName; }
    public String getParentTypeCode() { return parentTypeCode; }
    public Integer getLevelOrder() { return levelOrder; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }
    public String getDescription() { return description; }
    public boolean isAcademic() { return isAcademic; }
    public boolean isCanBeInspected() { return canBeInspected; }
    public boolean isCanHaveChildren() { return canHaveChildren; }
    public Integer getMaxDepth() { return maxDepth; }
    public boolean isSystem() { return isSystem; }
    public boolean isEnabled() { return isEnabled; }
    public Integer getSortOrder() { return sortOrder; }

    // Builder
    public static OrgUnitTypeEntityBuilder builder() { return new OrgUnitTypeEntityBuilder(); }

    public static class OrgUnitTypeEntityBuilder {
        private Long id;
        private String typeCode;
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder;
        private String icon;
        private String color;
        private String description;
        private boolean isAcademic;
        private boolean canBeInspected;
        private boolean canHaveChildren;
        private Integer maxDepth;
        private boolean isSystem;
        private boolean isEnabled;
        private Integer sortOrder;

        public OrgUnitTypeEntityBuilder id(Long id) { this.id = id; return this; }
        public OrgUnitTypeEntityBuilder typeCode(String typeCode) { this.typeCode = typeCode; return this; }
        public OrgUnitTypeEntityBuilder typeName(String typeName) { this.typeName = typeName; return this; }
        public OrgUnitTypeEntityBuilder parentTypeCode(String parentTypeCode) { this.parentTypeCode = parentTypeCode; return this; }
        public OrgUnitTypeEntityBuilder levelOrder(Integer levelOrder) { this.levelOrder = levelOrder; return this; }
        public OrgUnitTypeEntityBuilder icon(String icon) { this.icon = icon; return this; }
        public OrgUnitTypeEntityBuilder color(String color) { this.color = color; return this; }
        public OrgUnitTypeEntityBuilder description(String description) { this.description = description; return this; }
        public OrgUnitTypeEntityBuilder isAcademic(boolean isAcademic) { this.isAcademic = isAcademic; return this; }
        public OrgUnitTypeEntityBuilder canBeInspected(boolean canBeInspected) { this.canBeInspected = canBeInspected; return this; }
        public OrgUnitTypeEntityBuilder canHaveChildren(boolean canHaveChildren) { this.canHaveChildren = canHaveChildren; return this; }
        public OrgUnitTypeEntityBuilder maxDepth(Integer maxDepth) { this.maxDepth = maxDepth; return this; }
        public OrgUnitTypeEntityBuilder isSystem(boolean isSystem) { this.isSystem = isSystem; return this; }
        public OrgUnitTypeEntityBuilder isEnabled(boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public OrgUnitTypeEntityBuilder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }

        public OrgUnitTypeEntity build() {
            return new OrgUnitTypeEntity(id, typeCode, typeName, parentTypeCode, levelOrder, icon, color, description, isAcademic, canBeInspected, canHaveChildren, maxDepth, isSystem, isEnabled, sortOrder);
        }
    }

    /**
     * 更新基本信息
     */
    public void update(String typeName, String description, String icon, String color) {
        this.typeName = typeName;
        this.description = description;
        this.icon = icon;
        this.color = color;
    }

    /**
     * 更新组织特性
     */
    public void updateFeatures(boolean isAcademic, boolean canBeInspected, boolean canHaveChildren, Integer maxDepth) {
        this.isAcademic = isAcademic;
        this.canBeInspected = canBeInspected;
        this.canHaveChildren = canHaveChildren;
        this.maxDepth = maxDepth;
    }

    /**
     * 更新排序号
     */
    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 启用
     */
    public void enable() {
        this.isEnabled = true;
    }

    /**
     * 禁用
     */
    public void disable() {
        this.isEnabled = false;
    }

    /**
     * 是否顶级类型
     */
    public boolean isTopLevel() {
        return parentTypeCode == null || parentTypeCode.isEmpty();
    }

    /**
     * 是否职能部门类型
     */
    public boolean isFunctional() {
        return !isAcademic;
    }

    /**
     * 检查是否可以作为指定父类型的子级
     */
    public boolean canBeChildOf(OrgUnitTypeEntity parentType) {
        if (parentType == null) {
            return this.levelOrder == 1; // 顶级类型
        }

        // 父类型必须允许子级
        if (!parentType.isCanHaveChildren()) {
            return false;
        }

        // 检查层级关系
        return this.levelOrder > parentType.getLevelOrder();
    }
}
