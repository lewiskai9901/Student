package com.school.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批状态枚举
 */
@Getter
@AllArgsConstructor
public enum ApprovalStatus {

    PENDING(0, "待审批"),
    APPROVED(1, "通过"),
    REJECTED(2, "打回"),
    TRANSFERRED(3, "转交");

    private final Integer code;
    private final String desc;

    public static ApprovalStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ApprovalStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String getDescByCode(Integer code) {
        ApprovalStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知";
    }
}
