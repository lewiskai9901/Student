package com.school.management.domain.access.model;

/**
 * 数据权限范围枚举
 * 定义用户可访问的数据范围级别
 */
public enum DataScope {
    /**
     * 全部数据
     */
    ALL("all", "全部数据", "Access to all data"),

    /**
     * 本部门及以下
     */
    DEPARTMENT_AND_BELOW("department_and_below", "本部门及以下", "Access to department and subordinate data"),

    /**
     * 仅本部门
     */
    DEPARTMENT("department", "仅本部门", "Access to own department data"),

    /**
     * 本年级
     */
    GRADE("grade", "本年级", "Access to own grade data"),

    /**
     * 仅本班级
     */
    CLASS("class_only", "仅本班级", "Access to own class data"),

    /**
     * 自定义范围
     */
    CUSTOM("custom", "自定义范围", "Access to custom-defined data scope"),

    /**
     * 仅本人
     */
    SELF("self", "仅本人", "Access to self-created data only");

    private final String code;
    private final String displayName;
    private final String description;

    DataScope(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
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

    /**
     * 根据code查找DataScope
     */
    public static DataScope fromCode(String code) {
        for (DataScope scope : values()) {
            if (scope.code.equals(code)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown DataScope code: " + code);
    }
}
