package com.school.management.domain.inspection.model.v6;

/**
 * V6检查员分配模式
 */
public enum InspectorAssignmentMode {
    FREE("自由领取", "检查员自由领取任务"),
    ASSIGNED("指定分配", "管理员指定检查员"),
    HYBRID("混合模式", "部分指定，部分自由领取");

    private final String label;
    private final String description;

    InspectorAssignmentMode(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return name();
    }

    public static InspectorAssignmentMode fromCode(String code) {
        if (code == null) return FREE;
        for (InspectorAssignmentMode mode : values()) {
            if (mode.name().equalsIgnoreCase(code)) {
                return mode;
            }
        }
        return FREE;
    }
}
