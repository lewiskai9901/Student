package com.school.management.domain.teaching.model.task;

import lombok.Getter;

@Getter
public enum TaskStatus {
    PENDING(0, "待落实"),
    TEACHER_ASSIGNED(1, "已分配教师"),
    SCHEDULED(2, "已排课"),
    IN_PROGRESS(3, "进行中"),
    FINISHED(4, "已结束"),
    CANCELLED(9, "已取消");

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
