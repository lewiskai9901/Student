package com.school.management.domain.inspection.model.v6;

/**
 * V6整改状态枚举
 */
public enum CorrectiveActionStatus {
    /** 待整改 */
    PENDING("待整改"),

    /** 已提交整改 */
    SUBMITTED("已提交"),

    /** 已验收 */
    VERIFIED("已验收"),

    /** 已驳回 */
    REJECTED("已驳回"),

    /** 已取消 */
    CANCELLED("已取消");

    private final String description;

    CorrectiveActionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
