package com.school.management.domain.place.model.valueobject;

import java.util.List;
import java.util.Map;

/**
 * 场所基础分类枚举（统一类型系统 Phase 3）
 * 定义6种基础分类及其层级约束关系和默认行为特征
 */
public enum BaseCategory {

    SITE("校区/园区", List.of("BUILDING"),
            Map.of("hasCapacity", false, "bookable", false, "assignable", true, "occupiable", false)),
    BUILDING("楼栋", List.of("FLOOR"),
            Map.of("hasCapacity", false, "bookable", false, "assignable", true, "occupiable", false)),
    FLOOR("楼层", List.of("ROOM", "AREA"),
            Map.of("hasCapacity", false, "bookable", false, "assignable", false, "occupiable", false)),
    ROOM("房间", List.of(),
            Map.of("hasCapacity", true, "bookable", true, "assignable", true, "occupiable", true)),
    AREA("区域", List.of("POINT"),
            Map.of("hasCapacity", true, "bookable", false, "assignable", true, "occupiable", false)),
    POINT("点位", List.of(),
            Map.of("hasCapacity", false, "bookable", false, "assignable", false, "occupiable", false));

    private final String label;
    private final List<String> allowedChildCategories;
    private final Map<String, Boolean> defaultFeatures;

    BaseCategory(String label, List<String> allowedChildCategories, Map<String, Boolean> defaultFeatures) {
        this.label = label;
        this.allowedChildCategories = allowedChildCategories;
        this.defaultFeatures = defaultFeatures;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getAllowedChildCategories() {
        return allowedChildCategories;
    }

    public Map<String, Boolean> getDefaultFeatures() {
        return defaultFeatures;
    }

    public boolean canHaveChild(String childCategoryCode) {
        return allowedChildCategories.contains(childCategoryCode);
    }

    public boolean isLeaf() {
        return allowedChildCategories.isEmpty();
    }

    public boolean isRoot() {
        return this == SITE;
    }
}
