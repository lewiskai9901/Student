package com.school.management.casbin.enums;

import lombok.Getter;

/**
 * 数据范围类型枚举
 *
 * @author system
 * @version 1.0.0
 */
@Getter
public enum ScopeType {

    /**
     * 全部数据
     */
    ALL("ALL", "全部数据", "scope:*", 1),

    /**
     * 指定部门
     */
    DEPT("DEPT", "指定部门", "scope:dept:", 2),

    /**
     * 部门+年级交叉
     */
    DEPT_GRADE("DEPT_GRADE", "部门+年级", "scope:dept_grade:", 3),

    /**
     * 指定年级
     */
    GRADE("GRADE", "指定年级", "scope:grade:", 4),

    /**
     * 指定班级
     */
    CLASS("CLASS", "指定班级", "scope:class:", 5),

    /**
     * 仅本人数据
     */
    SELF("SELF", "仅本人", "scope:self", 6),

    /**
     * @deprecated 为兼容旧代码保留，请使用DEPT
     */
    @Deprecated
    DEPARTMENT("DEPARTMENT", "指定部门", "scope:dept:", 2);

    private final String code;
    private final String name;
    private final String prefix;
    private final int level;

    ScopeType(String code, String name, String prefix, int level) {
        this.code = code;
        this.name = name;
        this.prefix = prefix;
        this.level = level;
    }

    /**
     * 根据代码获取枚举
     */
    public static ScopeType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ScopeType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据范围表达式获取类型
     */
    public static ScopeType fromExpression(String expression) {
        if (expression == null) {
            return null;
        }
        if ("scope:*".equals(expression)) {
            return ALL;
        }
        if (expression.startsWith("scope:self")) {
            return SELF;
        }
        if (expression.startsWith("scope:dept_grade:")) {
            return DEPT_GRADE;
        }
        if (expression.startsWith("scope:dept:")) {
            return DEPT;
        }
        if (expression.startsWith("scope:grade:")) {
            return GRADE;
        }
        if (expression.startsWith("scope:class:")) {
            return CLASS;
        }
        return null;
    }

    /**
     * 构建范围表达式
     */
    public String buildExpression(Long... ids) {
        if (this == ALL) {
            return "scope:*";
        }
        if (this == SELF) {
            return "scope:self";
        }
        if (ids == null || ids.length == 0) {
            return prefix;
        }
        if (this == DEPT_GRADE && ids.length >= 2) {
            return prefix + ids[0] + ":" + ids[1];
        }
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(ids[i]);
        }
        return sb.toString();
    }
}
