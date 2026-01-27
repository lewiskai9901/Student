package com.school.management.domain.asset.model.valueobject;

import lombok.Getter;

/**
 * 审批类型
 */
@Getter
public enum ApprovalType {
    BORROW_APPLY(1, "借用申请"),
    PROCUREMENT(2, "采购申请"),
    SCRAP(3, "报废申请"),
    TRANSFER(4, "调拨申请");

    private final int code;
    private final String description;

    ApprovalType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ApprovalType fromCode(int code) {
        for (ApprovalType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ApprovalType code: " + code);
    }
}
