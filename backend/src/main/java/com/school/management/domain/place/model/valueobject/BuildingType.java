package com.school.management.domain.place.model.valueobject;

import lombok.Getter;

/**
 * 楼宇类型枚举
 */
@Getter
public enum BuildingType {
    TEACHING("教学楼"),
    DORMITORY("宿舍楼"),
    OFFICE("办公楼"),
    MIXED("综合楼");

    private final String description;

    BuildingType(String description) {
        this.description = description;
    }

    /**
     * 根据楼宇类型获取推荐的房间类型
     */
    public RoomType[] getRecommendedRoomTypes() {
        switch (this) {
            case TEACHING:
                return new RoomType[]{RoomType.CLASSROOM, RoomType.MULTIMEDIA,
                    RoomType.SMART_CLASSROOM, RoomType.LAB, RoomType.COMPUTER_LAB,
                    RoomType.TRAINING, RoomType.MEETING};
            case DORMITORY:
                return new RoomType[]{RoomType.DORMITORY, RoomType.STAFF_DORMITORY,
                    RoomType.UTILITY, RoomType.BATHROOM, RoomType.POWER_ROOM};
            case OFFICE:
                return new RoomType[]{RoomType.OFFICE, RoomType.MEETING,
                    RoomType.STORAGE, RoomType.UTILITY};
            case MIXED:
            default:
                return RoomType.values();
        }
    }
}
