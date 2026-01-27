package com.school.management.domain.asset.model.valueobject;

import lombok.Getter;

/**
 * 预警类型
 */
@Getter
public enum AlertType {
    OVERDUE(1, "借用逾期"),
    NEAR_OVERDUE(2, "即将逾期"),
    WARRANTY_EXPIRE(3, "保修到期"),
    LOW_STOCK(4, "库存不足");

    private final int code;
    private final String description;

    AlertType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AlertType fromCode(int code) {
        for (AlertType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AlertType code: " + code);
    }
}
