package com.school.management.domain.student.model;

/**
 * 班级状态枚举
 */
public enum ClassStatus {

    /**
     * 筹建中 - 班级刚创建，尚未开始招生
     */
    PREPARING("筹建中"),

    /**
     * 在读中 - 正常在校班级
     */
    ACTIVE("在读中"),

    /**
     * 已毕业 - 班级已完成学业
     */
    GRADUATED("已毕业"),

    /**
     * 已撤销 - 班级被撤销
     */
    DISSOLVED("已撤销");

    private final String description;

    ClassStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
