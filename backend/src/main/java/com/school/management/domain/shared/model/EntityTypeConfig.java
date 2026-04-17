package com.school.management.domain.shared.model;

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
 * 统一类型配置领域实体 — 替代 OrgType / UserType / UniversalPlaceType。
 * 对应表: entity_type_configs，按 entity_type 区分三个领域。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityTypeConfig implements ConfigurableType {

    private Long id;
    private Long tenantId;

    /** ORG_UNIT / PLACE / USER */
    private String entityType;

    private String typeCode;
    private String typeName;
    private String description;
    private String icon;
    private String category;
    private String parentTypeCode;

    private List<String> allowedChildTypeCodes;
    private Integer maxDepth;

    /** JSON Schema 原文 (fields: [...]) */
    private String metadataSchema;

    private Map<String, Boolean> features;
    private Map<String, Object> uiConfig;

    /** 跨引用 */
    private List<String> defaultRoleCodes;
    private List<String> defaultUserTypeCodes;
    private List<String> defaultOrgTypeCodes;
    private List<String> defaultPlaceTypeCodes;

    private boolean pluginRegistered;
    private String pluginClass;

    @JsonProperty("system")
    private boolean isSystem;

    @JsonProperty("enabled")
    private boolean isEnabled;

    private Integer sortOrder;

    @Override
    public Long getId() {
        return id;
    }

    public boolean allowsChildType(String childTypeCode) {
        return allowedChildTypeCodes != null && allowedChildTypeCodes.contains(childTypeCode);
    }

    @JsonIgnore
    public List<String> getDefaultRoleCodeList() {
        return defaultRoleCodes != null ? defaultRoleCodes : Collections.emptyList();
    }

    @JsonIgnore
    public List<String> getDefaultUserTypeCodeList() {
        return defaultUserTypeCodes != null ? defaultUserTypeCodes : Collections.emptyList();
    }

    @JsonIgnore
    public List<String> getDefaultOrgTypeCodeList() {
        return defaultOrgTypeCodes != null ? defaultOrgTypeCodes : Collections.emptyList();
    }

    @JsonIgnore
    public List<String> getDefaultPlaceTypeCodeList() {
        return defaultPlaceTypeCodes != null ? defaultPlaceTypeCodes : Collections.emptyList();
    }

    public void update(String typeName, String description) {
        this.typeName = typeName;
        this.description = description;
    }

    public void updateFeatures(Map<String, Boolean> features) {
        this.features = features;
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
