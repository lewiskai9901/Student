package com.school.management.domain.shared;

import java.util.List;
import java.util.Map;

/**
 * Unified contract for configurable type entities (OrgType, UserType, PlaceType).
 * Defines the 13 core fields shared across all type systems.
 */
public interface ConfigurableType extends Entity<Long> {

    String getTypeCode();
    String getTypeName();
    String getCategory();
    String getParentTypeCode();
    String getDescription();
    Map<String, Boolean> getFeatures();
    String getMetadataSchema();
    List<String> getAllowedChildTypeCodes();
    Integer getMaxDepth();
    boolean isSystem();
    boolean isEnabled();
    Integer getSortOrder();

    default boolean hasFeature(String key) {
        return getFeatures() != null && Boolean.TRUE.equals(getFeatures().get(key));
    }

    default boolean isTopLevel() {
        return getParentTypeCode() == null || getParentTypeCode().isEmpty();
    }
}
