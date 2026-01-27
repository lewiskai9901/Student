package com.school.management.casbin.model;

/**
 * 数据权限范围枚举
 * 定义用户可访问的数据范围级别
 *
 * 整数code用于数据库存储和V1 API兼容
 * 字符串code用于V2 API序列化
 */
public enum DataScope {
    /**
     * 全部数据
     */
    ALL(1, "all", "全部数据", "Access to all data"),

    /**
     * 本部门及以下
     */
    DEPARTMENT_AND_BELOW(6, "department_and_below", "本部门及以下", "Access to department and subordinate data"),

    /**
     * 仅本部门
     */
    DEPARTMENT(2, "department", "仅本部门", "Access to own department data"),

    /**
     * 自定义范围（支持多维度：组织单元+年级+班级）
     */
    CUSTOM(7, "custom", "自定义范围", "Access to custom-defined data scope with org units, grades, and classes"),

    /**
     * 仅本人
     */
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

    /**
     * 获取整数code（数据库存储和V1兼容）
     */
    public Integer getIntCode() {
        return intCode;
    }

    /**
     * 获取字符串code（V2 API序列化）
     */
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
     * 根据字符串code查找DataScope
     */
    public static DataScope fromCode(String code) {
        for (DataScope scope : values()) {
            if (scope.code.equals(code)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown DataScope code: " + code);
    }

    /**
     * 根据整数code查找DataScope（V1兼容）
     */
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
