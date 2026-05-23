package com.school.management.domain.inspection.exception;

/**
 * 调度组业务不变量违反: assignStrategy / scheduleMode 与其他字段冲突.
 */
public class InvalidPlanStateException extends RuntimeException {
    public InvalidPlanStateException(String message) {
        super(message);
    }
}
