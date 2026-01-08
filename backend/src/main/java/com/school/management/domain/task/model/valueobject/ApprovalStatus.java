package com.school.management.domain.task.model.valueobject;

/**
 * 审批状态值对象
 */
public enum ApprovalStatus {

    PENDING(0, "待审批"),
    APPROVED(1, "通过"),
    REJECTED(2, "打回"),
    TRANSFERRED(3, "转交");

    private final int code;
    private final String name;

    ApprovalStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ApprovalStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ApprovalStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown approval status code: " + code);
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isApproved() {
        return this == APPROVED;
    }

    public boolean isRejected() {
        return this == REJECTED;
    }
}
