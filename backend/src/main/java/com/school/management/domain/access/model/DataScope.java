package com.school.management.domain.access.model;

/**
 * 数据权限范围枚举
 * 定义用户可访问的数据范围级别
 */
public enum DataScope {
    ALL(1, "all", "全部数据", "Access to all data"),
    DEPARTMENT_AND_BELOW(6, "department_and_below", "本部门及以下", "Access to department and subordinate data"),
    DEPARTMENT(2, "department", "仅本部门", "Access to own department data"),
    CUSTOM(7, "custom", "自定义范围", "Access to custom-defined data scope with org units, grades, and classes"),
    SELF(5, "self", "仅本人", "Access to self-created data only");

    private final Integer intCode;
    private final String code;
    private final String displayName;
    private final String description;

    DataScope(Integer intCode, String code, String displayName, String description) {
        this.intCode = intCode;
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public Integer getIntCode() {
        return intCode;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static DataScope fromCode(String code) {
        for (DataScope scope : values()) {
            if (scope.code.equals(code)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown DataScope code: " + code);
    }

    public static DataScope fromCode(Integer intCode) {
        if (intCode == null) {
            return ALL;
        }
        for (DataScope scope : values()) {
            if (scope.intCode.equals(intCode)) {
                return scope;
            }
        }
        return ALL;
    }
}
