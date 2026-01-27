package com.school.management.domain.asset.model.valueobject;

import lombok.Getter;

/**
 * 维修状态值对象
 */
@Getter
public enum MaintenanceStatus {
    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成");

    private final int code;
    private final String description;

    MaintenanceStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static MaintenanceStatus fromCode(int code) {
        for (MaintenanceStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown maintenance status code: " + code);
    }
}
