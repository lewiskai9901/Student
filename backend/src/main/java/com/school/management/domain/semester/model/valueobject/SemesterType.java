package com.school.management.domain.semester.model.valueobject;

import com.school.management.domain.shared.ValueObject;

/**
 * 学期类型值对象
 */
public enum SemesterType implements ValueObject {

    FIRST(1, "第一学期"),
    SECOND(2, "第二学期");

    private final Integer code;
    private final String description;

    SemesterType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SemesterType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SemesterType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的学期类型码: " + code);
    }

    public boolean isFirst() {
        return this == FIRST;
    }

    public boolean isSecond() {
        return this == SECOND;
    }
}
