package com.school.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 日常检查状态枚举
 *
 * @author system
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum DailyCheckStatus {

    NOT_STARTED(0, "未开始"),
    IN_PROGRESS(1, "进行中"),
    SUBMITTED(2, "已提交"),
    PUBLISHED(3, "已发布");

    private final Integer code;
    private final String desc;

    public static DailyCheckStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DailyCheckStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String getDescByCode(Integer code) {
        DailyCheckStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知";
    }

    /**
     * 检查是否可以编辑（未开始或进行中状态可以编辑）
     */
    public static boolean canEdit(Integer code) {
        return NOT_STARTED.code.equals(code) || IN_PROGRESS.code.equals(code);
    }
}
