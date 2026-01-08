package com.school.management.domain.task.model;

/**
 * Task priority levels.
 */
public enum TaskPriority {

    /**
     * Urgent priority - requires immediate attention.
     */
    URGENT(1, "紧急"),

    /**
     * Normal priority - standard timeline.
     */
    NORMAL(2, "普通"),

    /**
     * Low priority - can be deferred.
     */
    LOW(3, "低");

    private final int value;
    private final String displayName;

    TaskPriority(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public int getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converts from integer value.
     *
     * @param value the integer value
     * @return corresponding TaskPriority
     */
    public static TaskPriority fromValue(Integer value) {
        if (value == null) return NORMAL;
        for (TaskPriority priority : values()) {
            if (priority.value == value) {
                return priority;
            }
        }
        return NORMAL;
    }
}
