package com.school.management.domain.inspection.model.v6;

/**
 * V6检查明细作用范围枚举
 */
public enum DetailScope {
    /** 整体 - 针对目标整体评价 */
    WHOLE("整体"),

    /** 个体 - 需关联到具体个体(学生/场所) */
    INDIVIDUAL("个体");

    private final String description;

    DetailScope(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
