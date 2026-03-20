package com.school.management.domain.user.model.entity;

import java.util.Map;

/**
 * 用户类型分类枚举
 * 与 OrgCategory 统一模式
 */
public enum UserCategory {

    ADMIN("管理员", Map.of(
            "requiresOrg", false,
            "requiresPlace", false
    )),

    STAFF("职工", Map.of(
            "requiresOrg", true,
            "requiresPlace", false
    )),

    MEMBER("成员", Map.of(
            "requiresOrg", true,
            "requiresPlace", true
    )),

    EXTERNAL("外部人员", Map.of(
            "requiresOrg", false,
            "requiresPlace", false
    ));

    private final String label;
    private final Map<String, Boolean> defaultFeatures;

    UserCategory(String label, Map<String, Boolean> defaultFeatures) {
        this.label = label;
        this.defaultFeatures = defaultFeatures;
    }

    public String getLabel() {
        return label;
    }

    public Map<String, Boolean> getDefaultFeatures() {
        return defaultFeatures;
    }
}
