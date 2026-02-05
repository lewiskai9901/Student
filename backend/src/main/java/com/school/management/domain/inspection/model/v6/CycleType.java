package com.school.management.domain.inspection.model.v6;

/**
 * V6检查周期类型
 */
public enum CycleType {
    DAILY("每日", "每天执行"),
    WEEKLY("每周", "每周指定日期执行"),
    MONTHLY("每月", "每月指定日期执行"),
    CUSTOM("自定义", "自定义周期");

    private final String label;
    private final String description;

    CycleType(String label, String description) {
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

    public static CycleType fromCode(String code) {
        if (code == null) return null;
        for (CycleType type : values()) {
            if (type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
