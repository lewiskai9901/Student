package com.school.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 *
 * @author system
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum UserStatus {

    DISABLED(0, "禁用"),
    ENABLED(1, "正常"),
    LOCKED(2, "锁定");

    private final Integer code;
    private final String desc;

    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String getDescByCode(Integer code) {
        UserStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知";
    }

    /**
     * 检查用户是否处于可用状态
     */
    public static boolean isEnabled(Integer code) {
        return ENABLED.code.equals(code);
    }
}
