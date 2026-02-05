package com.school.management.interfaces.rest.organization;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.List;

/**
 * 系统模块响应DTO
 */
public class SystemModuleResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String moduleCode;
    private String moduleName;
    private String moduleDesc;
    private String parentCode;
    private String icon;
    private Integer sortOrder;
    private Boolean isEnabled;
    private List<SystemModuleResponse> children;

    public SystemModuleResponse() {}

    public SystemModuleResponse(Long id, String moduleCode, String moduleName, String moduleDesc,
                                String parentCode, String icon, Integer sortOrder, Boolean isEnabled,
                                List<SystemModuleResponse> children) {
        this.id = id;
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.moduleDesc = moduleDesc;
        this.parentCode = parentCode;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.isEnabled = isEnabled;
        this.children = children;
    }

    // Getters
    public Long getId() { return id; }
    public String getModuleCode() { return moduleCode; }
    public String getModuleName() { return moduleName; }
    public String getModuleDesc() { return moduleDesc; }
    public String getParentCode() { return parentCode; }
    public String getIcon() { return icon; }
    public Integer getSortOrder() { return sortOrder; }
    public Boolean getIsEnabled() { return isEnabled; }
    public List<SystemModuleResponse> getChildren() { return children; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public void setModuleDesc(String moduleDesc) { this.moduleDesc = moduleDesc; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }
    public void setIcon(String icon) { this.icon = icon; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public void setChildren(List<SystemModuleResponse> children) { this.children = children; }

    // Builder
    public static SystemModuleResponseBuilder builder() {
        return new SystemModuleResponseBuilder();
    }

    public static class SystemModuleResponseBuilder {
        private Long id;
        private String moduleCode;
        private String moduleName;
        private String moduleDesc;
        private String parentCode;
        private String icon;
        private Integer sortOrder;
        private Boolean isEnabled;
        private List<SystemModuleResponse> children;

        public SystemModuleResponseBuilder id(Long id) { this.id = id; return this; }
        public SystemModuleResponseBuilder moduleCode(String moduleCode) { this.moduleCode = moduleCode; return this; }
        public SystemModuleResponseBuilder moduleName(String moduleName) { this.moduleName = moduleName; return this; }
        public SystemModuleResponseBuilder moduleDesc(String moduleDesc) { this.moduleDesc = moduleDesc; return this; }
        public SystemModuleResponseBuilder parentCode(String parentCode) { this.parentCode = parentCode; return this; }
        public SystemModuleResponseBuilder icon(String icon) { this.icon = icon; return this; }
        public SystemModuleResponseBuilder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public SystemModuleResponseBuilder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public SystemModuleResponseBuilder children(List<SystemModuleResponse> children) { this.children = children; return this; }

        public SystemModuleResponse build() {
            return new SystemModuleResponse(id, moduleCode, moduleName, moduleDesc, parentCode, icon, sortOrder, isEnabled, children);
        }
    }
}
