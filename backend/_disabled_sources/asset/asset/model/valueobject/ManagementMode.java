package com.school.management.domain.asset.model.valueobject;

import lombok.Getter;

/**
 * 资产管理模式值对象
 *
 * SINGLE_ITEM: 单品管理 - 每个资产有唯一编码，quantity固定为1
 * BATCH: 批量管理 - 一条记录代表多个同类资产，支持库存扣减
 */
@Getter
public enum ManagementMode {
    SINGLE_ITEM(1, "单品管理"),
    BATCH(2, "批量管理");

    private final int code;
    private final String description;

    ManagementMode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ManagementMode fromCode(int code) {
        for (ManagementMode mode : values()) {
            if (mode.code == code) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown management mode code: " + code);
    }

    /**
     * 是否为单品管理模式
     */
    public boolean isSingleItem() {
        return this == SINGLE_ITEM;
    }

    /**
     * 是否为批量管理模式
     */
    public boolean isBatch() {
        return this == BATCH;
    }
}
