package com.school.management.domain.organization.model.entity;

import com.school.management.domain.shared.Entity;

/**
 * 组织类型实体
 * 定义组织的层级类型（学校、院系、系部等）
 */
public class OrgType implements Entity<Long> {

    private Long id;
    private String typeCode;
    private String typeName;
    private String parentTypeCode;
    private Integer levelOrder;
    private String icon;
    private String color;
    private String description;

    // 特性配置
    private boolean canHaveClasses;
    private boolean canHaveStudents;
    private boolean canBeInspected;
    private boolean canHaveLeader;

    // 系统标识
    private boolean isSystem;
    private boolean isEnabled;
    private Integer sortOrder;

    protected OrgType() {}

    private OrgType(Builder builder) {
        this.id = builder.id;
        this.typeCode = builder.typeCode;
        this.typeName = builder.typeName;
        this.parentTypeCode = builder.parentTypeCode;
        this.levelOrder = builder.levelOrder;
        this.icon = builder.icon;
        this.color = builder.color;
        this.description = builder.description;
        this.canHaveClasses = builder.canHaveClasses;
        this.canHaveStudents = builder.canHaveStudents;
        this.canBeInspected = builder.canBeInspected;
        this.canHaveLeader = builder.canHaveLeader;
        this.isSystem = builder.isSystem;
        this.isEnabled = builder.isEnabled;
        this.sortOrder = builder.sortOrder;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getParentTypeCode() {
        return parentTypeCode;
    }

    public Integer getLevelOrder() {
        return levelOrder;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCanHaveClasses() {
        return canHaveClasses;
    }

    public boolean isCanHaveStudents() {
        return canHaveStudents;
    }

    public boolean isCanBeInspected() {
        return canBeInspected;
    }

    public boolean isCanHaveLeader() {
        return canHaveLeader;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 判断是否为顶级类型
     */
    public boolean isTopLevel() {
        return parentTypeCode == null || parentTypeCode.isEmpty();
    }

    /**
     * 更新类型信息
     */
    public void update(String typeName, String description, String icon, String color) {
        if (this.isSystem) {
            // 系统预置类型只允许修改部分属性
            this.description = description;
            this.icon = icon;
            this.color = color;
        } else {
            this.typeName = typeName;
            this.description = description;
            this.icon = icon;
            this.color = color;
        }
    }

    /**
     * 更新特性配置
     */
    public void updateFeatures(boolean canHaveClasses, boolean canHaveStudents,
                               boolean canBeInspected, boolean canHaveLeader) {
        if (!this.isSystem) {
            this.canHaveClasses = canHaveClasses;
            this.canHaveStudents = canHaveStudents;
            this.canBeInspected = canBeInspected;
            this.canHaveLeader = canHaveLeader;
        }
    }

    /**
     * 启用类型
     */
    public void enable() {
        this.isEnabled = true;
    }

    /**
     * 禁用类型
     */
    public void disable() {
        if (!this.isSystem) {
            this.isEnabled = false;
        }
    }

    /**
     * 更新排序
     */
    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String typeCode;
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder = 0;
        private String icon;
        private String color;
        private String description;
        private boolean canHaveClasses = false;
        private boolean canHaveStudents = false;
        private boolean canBeInspected = true;
        private boolean canHaveLeader = true;
        private boolean isSystem = false;
        private boolean isEnabled = true;
        private Integer sortOrder = 0;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder typeCode(String typeCode) {
            this.typeCode = typeCode;
            return this;
        }

        public Builder typeName(String typeName) {
            this.typeName = typeName;
            return this;
        }

        public Builder parentTypeCode(String parentTypeCode) {
            this.parentTypeCode = parentTypeCode;
            return this;
        }

        public Builder levelOrder(Integer levelOrder) {
            this.levelOrder = levelOrder;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder canHaveClasses(boolean canHaveClasses) {
            this.canHaveClasses = canHaveClasses;
            return this;
        }

        public Builder canHaveStudents(boolean canHaveStudents) {
            this.canHaveStudents = canHaveStudents;
            return this;
        }

        public Builder canBeInspected(boolean canBeInspected) {
            this.canBeInspected = canBeInspected;
            return this;
        }

        public Builder canHaveLeader(boolean canHaveLeader) {
            this.canHaveLeader = canHaveLeader;
            return this;
        }

        public Builder isSystem(boolean isSystem) {
            this.isSystem = isSystem;
            return this;
        }

        public Builder isEnabled(boolean isEnabled) {
            this.isEnabled = isEnabled;
            return this;
        }

        public Builder sortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public OrgType build() {
            return new OrgType(this);
        }
    }
}
