package com.school.management.domain.place.model.valueobject;

import lombok.Getter;

/**
 * 场所状态枚举
 */
@Getter
public enum PlaceStatus {
    DISABLED(0, "停用"),
    NORMAL(1, "正常"),
    MAINTENANCE(2, "维修中");

    private final int code;
    private final String description;

    PlaceStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PlaceStatus fromCode(int code) {
        for (PlaceStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid place status code: " + code);
    }

    public static PlaceStatus fromValue(int value) {
        return fromCode(value);
    }

    public int getValue() {
        return code;
    }

    public boolean isAvailable() {
        return this == NORMAL;
    }

    public boolean canCheckIn() {
        return this == NORMAL;
    }
}
