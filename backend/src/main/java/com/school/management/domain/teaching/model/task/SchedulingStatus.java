package com.school.management.domain.teaching.model.task;

import lombok.Getter;

@Getter
public enum SchedulingStatus {
    UNSCHEDULED(0, "未排课"),
    PARTIAL(1, "部分排课"),
    COMPLETE(2, "已完成");

    private final int code;
    private final String label;

    SchedulingStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static SchedulingStatus fromCode(int code) {
        for (SchedulingStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知的排课状态: " + code);
    }
}
