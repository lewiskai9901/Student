package com.school.management.domain.place.model.valueobject;

import lombok.Getter;

/**
 * 空间层级枚举
 * 用于PlaceCategory定义适用层级
 */
@Getter
public enum PlaceLevel {
    BUILDING("楼栋"),
    ROOM("房间");

    private final String description;

    PlaceLevel(String description) {
        this.description = description;
    }
}
