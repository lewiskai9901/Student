package com.school.management.domain.user.model.valueobject;

import com.school.management.domain.shared.ValueObject;

/**
 * 用户状态值对象
 */
public enum UserStatus implements ValueObject {

    ENABLED(1, "启用"),
    DISABLED(0, "禁用");

    private final int code;
    private final String description;

    UserStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            return ENABLED;
        }
        for (UserStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return ENABLED;
    }

    public boolean isEnabled() {
        return this == ENABLED;
    }
}
