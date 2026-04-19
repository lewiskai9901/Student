package com.school.management.domain.student.model.valueobject;

/**
 * 性别值对象
 */
public enum Gender {
    /**
     * 未知
     */
    UNKNOWN(0, "未知"),

    /**
     * 男
     */
    MALE(1, "男"),

    /**
     * 女
     */
    FEMALE(2, "女");

    private final int code;
    private final String description;

    Gender(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return description;
    }

    public static Gender fromCode(int code) {
        for (Gender gender : values()) {
            if (gender.code == code) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Unknown gender code: " + code);
    }
}
