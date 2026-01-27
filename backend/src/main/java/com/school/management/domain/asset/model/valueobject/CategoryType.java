package com.school.management.domain.asset.model.valueobject;

import lombok.Getter;

/**
 * 资产分类类型值对象
 *
 * FIXED_ASSET: 固定资产 - 默认单品管理，如电脑、投影仪
 * LOW_VALUE: 低值易耗品 - 可选单品/批量管理，如桌椅
 * CONSUMABLE: 消耗品 - 默认批量管理，如文具、清洁用品
 */
@Getter
public enum CategoryType {
    FIXED_ASSET(1, "固定资产"),
    LOW_VALUE(2, "低值易耗品"),
    CONSUMABLE(3, "消耗品");

    private final int code;
    private final String description;

    CategoryType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CategoryType fromCode(int code) {
        for (CategoryType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown category type code: " + code);
    }

    /**
     * 获取该分类类型的默认管理模式
     */
    public ManagementMode getDefaultManagementMode() {
        return switch (this) {
            case FIXED_ASSET -> ManagementMode.SINGLE_ITEM;
            case LOW_VALUE -> ManagementMode.SINGLE_ITEM;  // 可选，默认单品
            case CONSUMABLE -> ManagementMode.BATCH;
        };
    }

    /**
     * 是否支持批量管理
     */
    public boolean supportsBatchManagement() {
        return this == LOW_VALUE || this == CONSUMABLE;
    }
}
