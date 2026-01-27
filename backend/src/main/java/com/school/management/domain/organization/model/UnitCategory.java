package com.school.management.domain.organization.model;

import lombok.Getter;

/**
 * 组织类别枚举
 */
@Getter
public enum UnitCategory {

    ACADEMIC("academic", "教学单位"),
    FUNCTIONAL("functional", "职能部门"),
    ADMINISTRATIVE("administrative", "行政单位");

    private final String code;
    private final String name;

    UnitCategory(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static UnitCategory fromCode(String code) {
        if (code == null) return ACADEMIC;
        for (UnitCategory category : values()) {
            if (category.code.equalsIgnoreCase(code)) {
                return category;
            }
        }
        return ACADEMIC;
    }
}
