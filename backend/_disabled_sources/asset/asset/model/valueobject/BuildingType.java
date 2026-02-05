package com.school.management.domain.asset.model.valueobject;

/**
 * 楼宇类型值对象
 */
public enum BuildingType {

    TEACHING(1, "教学楼"),
    DORMITORY(2, "宿舍楼"),
    OFFICE(3, "办公楼");

    private final int code;
    private final String name;

    BuildingType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static BuildingType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (BuildingType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown building type code: " + code);
    }

    public boolean isDormitory() {
        return this == DORMITORY;
    }

    public boolean isTeaching() {
        return this == TEACHING;
    }
}
