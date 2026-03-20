package com.school.management.domain.place.model.valueobject;

import lombok.Getter;

/**
 * 场所类型枚举
 */
@Getter
public enum PlaceType {
    CAMPUS("校区", 1),
    BUILDING("楼宇", 2),
    FLOOR("楼层", 3),
    ROOM("房间", 4);

    private final String description;
    private final int level;

    PlaceType(String description, int level) {
        this.description = description;
        this.level = level;
    }

    public boolean isBuilding() {
        return this == BUILDING;
    }

    public boolean isFloor() {
        return this == FLOOR;
    }

    public boolean isRoom() {
        return this == ROOM;
    }

    public boolean isCampus() {
        return this == CAMPUS;
    }
}
