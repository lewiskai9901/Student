package com.school.management.domain.inspection.model.v6;

/**
 * V6检查目标状态
 */
public enum TargetStatus {
    PENDING("待检查"),
    LOCKED("锁定中"),
    COMPLETED("已完成"),
    SKIPPED("已跳过");

    private final String label;

    TargetStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return name();
    }

    public static TargetStatus fromCode(String code) {
        if (code == null) return null;
        for (TargetStatus status : values()) {
            if (status.name().equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
