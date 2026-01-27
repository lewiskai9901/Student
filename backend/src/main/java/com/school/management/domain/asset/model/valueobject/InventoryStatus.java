package com.school.management.domain.asset.model.valueobject;

import lombok.Getter;

/**
 * 盘点状态值对象
 */
@Getter
public enum InventoryStatus {
    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String description;

    InventoryStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static InventoryStatus fromCode(int code) {
        for (InventoryStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown inventory status code: " + code);
    }
}
