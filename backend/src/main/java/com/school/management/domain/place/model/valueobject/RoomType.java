package com.school.management.domain.place.model.valueobject;

import lombok.Getter;

/**
 * 房间类型枚举
 */
@Getter
public enum RoomType {
    // 宿舍类
    DORMITORY("学生宿舍", true, true),
    STAFF_DORMITORY("教职工宿舍", true, false),

    // 教室类
    CLASSROOM("普通教室", false, false),
    MULTIMEDIA("多媒体教室", false, false),
    SMART_CLASSROOM("智慧教室", false, false),

    // 实验室类
    LAB("实验室", false, false),
    COMPUTER_LAB("计算机房", false, false),
    TRAINING("实训室", false, false),

    // 办公类
    OFFICE("办公室", true, false),
    MEETING("会议室", false, false),

    // 其他
    LIBRARY("图书馆/阅览室", false, false),
    STORAGE("仓库", false, false),
    UTILITY("功能房", false, false),
    BATHROOM("卫生间", false, true),
    POWER_ROOM("配电室", false, false);

    private final String description;
    private final boolean hasOccupancy;  // 是否有入住/使用人员
    private final boolean hasGender;     // 是否区分性别

    RoomType(String description, boolean hasOccupancy, boolean hasGender) {
        this.description = description;
        this.hasOccupancy = hasOccupancy;
        this.hasGender = hasGender;
    }

    /**
     * 是否是宿舍类型
     */
    public boolean isDormitory() {
        return this == DORMITORY || this == STAFF_DORMITORY;
    }

    /**
     * 是否是教室类型
     */
    public boolean isClassroom() {
        return this == CLASSROOM || this == MULTIMEDIA || this == SMART_CLASSROOM;
    }

    /**
     * 是否是实验室类型
     */
    public boolean isLab() {
        return this == LAB || this == COMPUTER_LAB || this == TRAINING;
    }

    /**
     * 是否是办公类型
     */
    public boolean isOffice() {
        return this == OFFICE || this == MEETING;
    }

    /**
     * 是否有容量限制
     */
    public boolean hasCapacity() {
        return this != UTILITY && this != POWER_ROOM;
    }
}
