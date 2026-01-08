package com.school.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态枚举
 */
@Getter
@AllArgsConstructor
public enum TaskStatus {

    PENDING(0, "待接收"),
    IN_PROGRESS(1, "进行中"),
    SUBMITTED(2, "待审核"),
    COMPLETED(3, "已完成"),
    REJECTED(4, "已打回"),
    CANCELLED(5, "已取消"),
    APPROVING(6, "审批中");

    private final Integer code;
    private final String desc;

    public static TaskStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TaskStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String getDescByCode(Integer code) {
        TaskStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知";
    }
}
