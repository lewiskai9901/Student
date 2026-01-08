package com.school.management.domain.asset.model.valueobject;

/**
 * 宿舍状态值对象
 */
public enum DormitoryStatus {

    ACTIVE(1, "正常"),
    INACTIVE(0, "停用");

    private final int code;
    private final String name;

    DormitoryStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static DormitoryStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DormitoryStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown dormitory status code: " + code);
    }

    public boolean isActive() {
        return this == ACTIVE;
    }
}
