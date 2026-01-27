package com.school.management.domain.task.model.valueobject;

/**
 * 任务状态值对象
 */
public enum TaskStatus {

    PENDING(0, "待接收"),
    IN_PROGRESS(1, "进行中"),
    SUBMITTED(2, "已提交"),
    COMPLETED(3, "已完成"),
    REJECTED(4, "已拒绝");

    private final int code;
    private final String name;

    TaskStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static TaskStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown task status code: " + code);
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isInProgress() {
        return this == IN_PROGRESS;
    }

    public boolean isSubmitted() {
        return this == SUBMITTED;
    }

    public boolean isCompleted() {
        return this == COMPLETED;
    }

    public boolean isRejected() {
        return this == REJECTED;
    }

    public boolean canAccept() {
        return this == PENDING;
    }

    public boolean canSubmit() {
        return this == IN_PROGRESS || this == REJECTED;
    }

    public boolean canApprove() {
        return this == SUBMITTED;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == REJECTED;
    }
}
