package com.school.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 申诉状态枚举
 * 1=待审核, 2=审核通过, 3=审核驳回, 4=已撤销, 5=已过期, 6=公示中, 7=已生效
 *
 * @author system
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum AppealStatus {

    PENDING(1, "待审核"),
    APPROVED(2, "审核通过"),
    REJECTED(3, "审核驳回"),
    WITHDRAWN(4, "已撤销"),
    EXPIRED(5, "已过期"),
    IN_PUBLICITY(6, "公示中"),
    EFFECTIVE(7, "已生效");

    private final Integer code;
    private final String desc;

    public static AppealStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AppealStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String getDescByCode(Integer code) {
        AppealStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知";
    }

    /**
     * 检查是否可以撤回申诉（只有待审核状态可以撤回）
     */
    public static boolean canWithdraw(Integer code) {
        return PENDING.code.equals(code);
    }

    /**
     * 检查是否可以审核（只有待审核状态可以审核）
     */
    public static boolean canReview(Integer code) {
        return PENDING.code.equals(code);
    }

    /**
     * 检查是否为终态（已撤销、已过期、已生效）
     */
    public static boolean isFinalState(Integer code) {
        return WITHDRAWN.code.equals(code)
            || EXPIRED.code.equals(code)
            || EFFECTIVE.code.equals(code);
    }
}
