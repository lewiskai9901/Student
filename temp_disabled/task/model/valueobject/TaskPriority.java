package com.school.management.domain.task.model.valueobject;

/**
 * 任务优先级值对象
 */
public enum TaskPriority {

    URGENT(1, "紧急"),
    NORMAL(2, "普通"),
    LOW(3, "低");

    private final int code;
    private final String name;

    TaskPriority(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static TaskPriority fromCode(Integer code) {
        if (code == null) {
            return NORMAL;
        }
        for (TaskPriority priority : values()) {
            if (priority.code == code) {
                return priority;
            }
        }
        return NORMAL;
    }

    public boolean isUrgent() {
        return this == URGENT;
    }
}
