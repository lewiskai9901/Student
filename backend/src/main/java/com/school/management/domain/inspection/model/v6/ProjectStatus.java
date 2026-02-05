package com.school.management.domain.inspection.model.v6;

/**
 * V6检查项目状态
 */
public enum ProjectStatus {
    DRAFT("草稿"),
    ACTIVE("进行中"),
    PAUSED("暂停"),
    COMPLETED("已完成"),
    ARCHIVED("已归档");

    private final String label;

    ProjectStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return name();
    }

    public static ProjectStatus fromCode(String code) {
        if (code == null) return null;
        for (ProjectStatus status : values()) {
            if (status.name().equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
