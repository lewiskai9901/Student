package com.school.management.domain.asset.model.valueobject;

import lombok.Getter;

/**
 * 预警级别
 */
@Getter
public enum AlertLevel {
    NORMAL(1, "普通"),
    IMPORTANT(2, "重要"),
    URGENT(3, "紧急");

    private final int code;
    private final String description;

    AlertLevel(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AlertLevel fromCode(int code) {
        for (AlertLevel level : values()) {
            if (level.code == code) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown AlertLevel code: " + code);
    }
}
