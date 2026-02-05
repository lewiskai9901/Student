package com.school.management.domain.asset.model.valueobject;

/**
 * 性别类型值对象（用于宿舍分配）
 */
public enum GenderType {

    MALE(1, "男"),
    FEMALE(2, "女"),
    MIXED(3, "混合");

    private final int code;
    private final String name;

    GenderType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static GenderType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (GenderType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown gender type code: " + code);
    }

    public boolean isMale() {
        return this == MALE;
    }

    public boolean isFemale() {
        return this == FEMALE;
    }

    public boolean isMixed() {
        return this == MIXED;
    }
}
