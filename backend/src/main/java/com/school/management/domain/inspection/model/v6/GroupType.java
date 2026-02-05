package com.school.management.domain.inspection.model.v6;

/**
 * V6分组类型枚举
 */
public enum GroupType {
    /** 动态分组 - 基于筛选条件自动计算成员 */
    DYNAMIC("动态"),

    /** 静态分组 - 手动指定成员列表 */
    STATIC("静态");

    private final String description;

    GroupType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
