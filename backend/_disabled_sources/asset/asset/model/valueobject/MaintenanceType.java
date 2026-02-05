package com.school.management.domain.asset.model.valueobject;

import lombok.Getter;

/**
 * 维修保养类型值对象
 */
@Getter
public enum MaintenanceType {
    REPAIR(1, "维修"),
    MAINTENANCE(2, "保养");

    private final int code;
    private final String description;

    MaintenanceType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static MaintenanceType fromCode(int code) {
        for (MaintenanceType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown maintenance type code: " + code);
    }
}
