package com.school.management.domain.inspection.model.v6;

/**
 * V6检查任务状态
 */
public enum TaskStatus {
    PENDING("待执行"),
    IN_PROGRESS("进行中"),
    SUBMITTED("已提交"),
    REVIEWED("已审核"),
    PUBLISHED("已发布"),
    CANCELLED("已取消");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return name();
    }

    public static TaskStatus fromCode(String code) {
        if (code == null) return null;
        for (TaskStatus status : values()) {
            if (status.name().equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
