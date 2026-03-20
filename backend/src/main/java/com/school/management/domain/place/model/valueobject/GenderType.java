package com.school.management.domain.place.model.valueobject;

import lombok.Getter;

/**
 * 性别类型枚举
 */
@Getter
public enum GenderType {
    MIXED(0, "不限"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final int code;
    private final String description;

    GenderType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static GenderType fromCode(Integer code) {
        if (code == null) {
            return MIXED; // 默认不限
        }
        for (GenderType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return MIXED; // 未知值默认为不限
    }

    /**
     * 检查学生性别是否与房间性别类型匹配
     * @param studentGender 学生性别字符串 ("男"/"女")
     * @return 是否匹配
     */
    public boolean matchesStudentGender(String studentGender) {
        if (this == MIXED) {
            return true;
        }
        if (studentGender == null) {
            return false;
        }
        return this.description.equals(studentGender);
    }

    /**
     * 是否限制性别
     */
    public boolean isRestricted() {
        return this != MIXED;
    }
}
