package com.school.management.domain.inspection.model.v6;

/**
 * V6检查范围类型
 */
public enum ScopeType {
    ORG("组织", "按组织结构检查"),
    PLACE("场所", "按场所检查"),
    USER("用户", "按用户检查");

    private final String label;
    private final String description;

    ScopeType(String label, String description) {
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

    public static ScopeType fromCode(String code) {
        if (code == null) return null;
        for (ScopeType type : values()) {
            if (type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
