package com.school.management.domain.space.model.valueobject;

import lombok.Getter;

/**
 * 空间层级枚举
 * 用于SpaceCategory定义适用层级
 */
@Getter
public enum SpaceLevel {
    BUILDING("楼栋"),
    ROOM("房间");

    private final String description;

    SpaceLevel(String description) {
        this.description = description;
    }
}
