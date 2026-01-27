package com.school.management.domain.organization.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统模块实体
 */
public class SystemModule {

    private Long id;
    private String moduleCode;
    private String moduleName;
    private String moduleDesc;
    private String parentCode;
    private String icon;
    private Integer sortOrder;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SystemModule> children = new ArrayList<>();

    public SystemModule() {
    }

    public void addChild(SystemModule child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }

    public boolean isTopLevel() {
        return this.parentCode == null || this.parentCode.isEmpty();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleDesc() {
        return moduleDesc;
    }

    public void setModuleDesc(String moduleDesc) {
        this.moduleDesc = moduleDesc;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<SystemModule> getChildren() {
        return children;
    }

    public void setChildren(List<SystemModule> children) {
        this.children = children;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SystemModule instance = new SystemModule();

        public Builder id(Long id) {
            instance.id = id;
            return this;
        }

        public Builder moduleCode(String moduleCode) {
            instance.moduleCode = moduleCode;
            return this;
        }

        public Builder moduleName(String moduleName) {
            instance.moduleName = moduleName;
            return this;
        }

        public Builder moduleDesc(String moduleDesc) {
            instance.moduleDesc = moduleDesc;
            return this;
        }

        public Builder parentCode(String parentCode) {
            instance.parentCode = parentCode;
            return this;
        }

        public Builder icon(String icon) {
            instance.icon = icon;
            return this;
        }

        public Builder sortOrder(Integer sortOrder) {
            instance.sortOrder = sortOrder;
            return this;
        }

        public Builder isEnabled(Boolean isEnabled) {
            instance.isEnabled = isEnabled;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            instance.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            instance.updatedAt = updatedAt;
            return this;
        }

        public Builder children(List<SystemModule> children) {
            instance.children = children;
            return this;
        }

        public SystemModule build() {
            return instance;
        }
    }
}
