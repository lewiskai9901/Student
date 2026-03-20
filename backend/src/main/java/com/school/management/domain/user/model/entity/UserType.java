package com.school.management.domain.user.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.school.management.domain.shared.ConfigurableType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 用户类型领域实体（统一类型系统 Phase 2）
 * 与 OrgType 统一模式：category + features + cross-references
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserType implements ConfigurableType {

    private Long id;
    private String typeCode;
    private String typeName;
    private String category;
    private String parentTypeCode;
    private String icon;
    private String description;

    /** 行为特征 JSON: {requiresOrg: bool, requiresPlace: bool, ...} */
    private Map<String, Boolean> features;

    /** 动态属性定义 JSON Schema */
    private String metadataSchema;

    /** 允许的子类型编码 */
    private List<String> allowedChildTypeCodes;

    /** 最大嵌套深度 */
    private Integer maxDepth;

    /** 默认角色编码 */
    private List<String> defaultRoleCodes;

    /** 关联组织类型编码 */
    private List<String> defaultOrgTypeCodes;

    /** 关联场所类型编码 */
    private List<String> defaultPlaceTypeCodes;

    @JsonProperty("system")
    private boolean isSystem;

    @JsonProperty("enabled")
    private boolean isEnabled;

    private Integer sortOrder;

    @Override
    public Long getId() {
        return id;
    }

    // ========== 行为特征查询 ==========

    @JsonIgnore
    public boolean isRequiresOrg() {
        return hasFeature("requiresOrg");
    }

    @JsonIgnore
    public boolean isRequiresPlace() {
        return hasFeature("requiresPlace");
    }

    public boolean allowsChildType(String childTypeCode) {
        return allowedChildTypeCodes != null && allowedChildTypeCodes.contains(childTypeCode);
    }

    @JsonIgnore
    public List<String> getDefaultRoleCodeList() {
        return defaultRoleCodes != null ? defaultRoleCodes : Collections.emptyList();
    }

    // ========== 领域行为 ==========

    public void update(String typeName, String description) {
        this.typeName = typeName;
        this.description = description;
    }

    public void updateFeatures(Map<String, Boolean> features) {
        this.features = features;
    }

    public void updateDefaultRoles(List<String> defaultRoleCodes) {
        this.defaultRoleCodes = defaultRoleCodes;
    }

    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void enable() {
        this.isEnabled = true;
    }

    public void disable() {
        this.isEnabled = false;
    }
}
