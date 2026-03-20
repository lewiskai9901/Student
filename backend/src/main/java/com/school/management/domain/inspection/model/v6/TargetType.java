package com.school.management.domain.inspection.model.v6;

/**
 * V6检查目标类型
 */
public enum TargetType {
    ORG("组织"),
    PLACE("场所"),
    USER("用户");

    private final String label;

    TargetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return name();
    }

    public static TargetType fromCode(String code) {
        if (code == null) return null;
        for (TargetType type : values()) {
            if (type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
