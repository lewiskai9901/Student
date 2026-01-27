package com.school.management.domain.asset.model.valueobject;

import lombok.Getter;

/**
 * 资产状态值对象
 */
@Getter
public enum AssetStatus {
    IN_USE(1, "在用"),
    IDLE(2, "闲置"),
    REPAIRING(3, "维修中"),
    SCRAPPED(4, "已报废");

    private final int code;
    private final String description;

    AssetStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AssetStatus fromCode(int code) {
        for (AssetStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown asset status code: " + code);
    }
}
