package com.school.management.domain.student.model.valueobject;

/**
 * 学籍状态值对象
 */
public enum StudentStatus {
    /**
     * 在读
     */
    STUDYING(1, "在读"),

    /**
     * 休学
     */
    SUSPENDED(2, "休学"),

    /**
     * 退学
     */
    WITHDRAWN(3, "退学"),

    /**
     * 毕业
     */
    GRADUATED(4, "毕业"),

    /**
     * 开除
     */
    EXPELLED(5, "开除");

    private final int code;
    private final String description;

    StudentStatus(int code, String description) {
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

    public static StudentStatus fromCode(int code) {
        for (StudentStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown student status code: " + code);
    }

    /**
     * 是否为活跃状态（可参与正常教学活动）
     */
    public boolean isActive() {
        return this == STUDYING;
    }

    /**
     * 是否为终态（不可变更）
     */
    public boolean isFinal() {
        return this == GRADUATED || this == EXPELLED;
    }
}
