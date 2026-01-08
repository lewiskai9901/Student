package com.school.management.enums;

import lombok.Getter;

/**
 * 数据范围类型枚举
 *
 * 用于 user_data_scopes 表，定义用户可管理的数据维度
 *
 * @author system
 * @version 3.0.0
 * @since 2024-01-01
 */
@Getter
public enum ScopeType {

    /**
     * 部门范围 - 最高级
     * 包含该部门下所有年级和班级
     */
    DEPARTMENT("DEPARTMENT", "部门", 1),

    /**
     * 年级范围 - 中间级
     * 包含该年级下所有班级
     */
    GRADE("GRADE", "年级", 2),

    /**
     * 班级范围 - 最低级
     * 仅包含该班级
     */
    CLASS("CLASS", "班级", 3);

    /**
     * 类型代码（存储在数据库中）
     */
    private final String code;

    /**
     * 类型名称（显示用）
     */
    private final String name;

    /**
     * 层级（数字越小权限越大）
     * 用于判断权限继承关系
     */
    private final int level;

    ScopeType(String code, String name, int level) {
        this.code = code;
        this.name = name;
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
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断当前类型是否包含目标类型
     * 例如：DEPARTMENT 包含 GRADE 和 CLASS
     *       GRADE 包含 CLASS
     *       CLASS 不包含其他类型
     */
    public boolean contains(ScopeType target) {
        if (target == null) {
            return false;
        }
        return this.level < target.level;
    }
}
