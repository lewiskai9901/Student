package com.school.management.enums;

/**
 * 标准人数来源枚举
 * 决定创建检查记录时使用什么标准人数
 *
 * @author system
 * @version 5.0.0
 * @since 2025-11-14
 */
public enum StandardSizeMode {

    /**
     * 固定标准 - 使用管理员手动设置的固定标准人数
     * 创建检查时:使用配置的固定值(如40人)
     * 所有记录自动保存快照,确保历史数据准确
     */
    FIXED("FIXED", "固定标准", "使用手动设置的固定标准人数(如每班40人),创建时自动保存快照"),

    /**
     * 实时平均 - 自动计算当时的实际平均班级人数
     * 创建检查时:自动计算同年级同部门的平均人数
     * 所有记录自动保存快照,确保历史数据准确
     */
    DYNAMIC("DYNAMIC", "实时平均", "自动计算创建时的实际平均班级人数,创建时自动保存快照");

    private final String code;
    private final String name;
    private final String description;

    StandardSizeMode(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static StandardSizeMode fromCode(String code) {
        if (code == null) {
            return DYNAMIC; // 默认动态模式
        }
        for (StandardSizeMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        return DYNAMIC;
    }

    /**
     * 判断是否为固定模式
     */
    public boolean isFixed() {
        return this == FIXED;
    }

    /**
     * 判断是否为动态模式
     */
    public boolean isDynamic() {
        return this == DYNAMIC;
    }

}
