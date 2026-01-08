package com.school.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务优先级枚举
 */
@Getter
@AllArgsConstructor
public enum TaskPriority {

    URGENT(1, "紧急"),
    NORMAL(2, "普通"),
    LOW(3, "低");

    private final Integer code;
    private final String desc;

    public static TaskPriority fromCode(Integer code) {
        if (code == null) {
            return NORMAL;
        }
        for (TaskPriority priority : values()) {
            if (priority.getCode().equals(code)) {
                return priority;
            }
        }
        return NORMAL;
    }

    public static String getDescByCode(Integer code) {
        TaskPriority priority = fromCode(code);
        return priority != null ? priority.getDesc() : "普通";
    }
}
