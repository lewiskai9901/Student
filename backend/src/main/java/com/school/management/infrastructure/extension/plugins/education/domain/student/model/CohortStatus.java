package com.school.management.domain.student.model;

/**
 * 年级状态枚举
 */
public enum CohortStatus {

    /**
     * 招生中 - 新年级，正在招生
     */
    ENROLLING("招生中"),

    /**
     * 在读 - 正常教学状态
     */
    ACTIVE("在读"),

    /**
     * 停招 - 停止招生但未毕业
     */
    SUSPENDED("停招"),

    /**
     * 已毕业
     */
    GRADUATED("已毕业");

    private final String displayName;

    CohortStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 从数字状态码转换（兼容V1）
     */
    public static CohortStatus fromLegacyStatus(Integer status) {
        if (status == null) {
            return ENROLLING;
        }
        switch (status) {
            case 1:
                return ACTIVE;
            case 2:
                return GRADUATED;
            case 3:
                return SUSPENDED;
            default:
                return ENROLLING;
        }
    }

    /**
     * 转换为数字状态码（兼容V1）
     */
    public Integer toLegacyStatus() {
        switch (this) {
            case ACTIVE:
                return 1;
            case GRADUATED:
                return 2;
            case SUSPENDED:
                return 3;
            case ENROLLING:
            default:
                return 0;
        }
    }
}
