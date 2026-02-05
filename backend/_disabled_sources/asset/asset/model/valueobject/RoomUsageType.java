package com.school.management.domain.asset.model.valueobject;

/**
 * 房间用途类型值对象
 */
public enum RoomUsageType {

    STUDENT_DORMITORY(1, "学生宿舍"),
    STAFF_DORMITORY(2, "教职工宿舍"),
    POWER_ROOM(3, "配电室"),
    BATHROOM(4, "卫生间"),
    STORAGE(5, "杂物间"),
    OTHER(6, "其他");

    private final int code;
    private final String name;

    RoomUsageType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static RoomUsageType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RoomUsageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown room usage type code: " + code);
    }

    public boolean canHaveBeds() {
        return this == STUDENT_DORMITORY || this == STAFF_DORMITORY;
    }

    public boolean isStudentDormitory() {
        return this == STUDENT_DORMITORY;
    }
}
