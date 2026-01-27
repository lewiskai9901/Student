package com.school.management.domain.asset.model.valueobject;

import lombok.Getter;

/**
 * 借用状态
 */
@Getter
public enum BorrowStatus {
    BORROWED(1, "借出中"),
    RETURNED(2, "已归还"),
    OVERDUE(3, "已逾期"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String description;

    BorrowStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static BorrowStatus fromCode(int code) {
        for (BorrowStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown BorrowStatus code: " + code);
    }
}
