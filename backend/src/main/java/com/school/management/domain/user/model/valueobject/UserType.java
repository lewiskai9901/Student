package com.school.management.domain.user.model.valueobject;

import com.school.management.domain.shared.ValueObject;

/**
 * 用户类型值对象
 */
public enum UserType implements ValueObject {

    ADMIN(1, "管理员"),
    TEACHER(2, "教师"),
    STUDENT(3, "学生");

    private final int code;
    private final String description;

    UserType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserType fromCode(Integer code) {
        if (code == null) {
            return TEACHER;
        }
        for (UserType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return TEACHER;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isTeacher() {
        return this == TEACHER;
    }

    public boolean isStudent() {
        return this == STUDENT;
    }
}
