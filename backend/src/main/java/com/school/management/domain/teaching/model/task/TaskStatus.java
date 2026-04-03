package com.school.management.domain.teaching.model.task;

import lombok.Getter;

@Getter
public enum TaskStatus {
    DRAFT(0, "草稿"),
    CONFIRMED(1, "已确认"),
    CANCELLED(2, "已取消");

    private final int code;
    private final String label;

    TaskStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static TaskStatus fromCode(int code) {
        for (TaskStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知的任务状态: " + code);
    }
}
