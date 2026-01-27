package com.school.management.domain.task.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Task status with state machine transitions.
 *
 * <p>State machine:
 * <pre>
 *     PENDING_ACCEPT → IN_PROGRESS → SUBMITTED → APPROVED → COMPLETED
 *                  ↘               ↘          ↘
 *                   CANCELLED      REJECTED    REJECTED
 * </pre>
 */
public enum TaskStatus {

    /**
     * Task created, waiting for assignee to accept.
     */
    PENDING_ACCEPT("待接收"),

    /**
     * Task accepted and in progress.
     */
    IN_PROGRESS("进行中"),

    /**
     * Task submitted for review.
     */
    SUBMITTED("已提交"),

    /**
     * Task approved by reviewer.
     */
    APPROVED("已审批"),

    /**
     * Task completed.
     */
    COMPLETED("已完成"),

    /**
     * Task rejected by reviewer.
     */
    REJECTED("已拒绝"),

    /**
     * Task cancelled by creator.
     */
    CANCELLED("已取消");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the allowed transitions from this status.
     *
     * @return set of allowed next statuses
     */
    public Set<TaskStatus> getAllowedTransitions() {
        return switch (this) {
            case PENDING_ACCEPT -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> EnumSet.of(SUBMITTED, CANCELLED);
            case SUBMITTED -> EnumSet.of(APPROVED, REJECTED);
            case APPROVED -> EnumSet.of(COMPLETED);
            case REJECTED -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(TaskStatus.class);
        };
    }

    /**
     * Checks if this status can transition to the target status.
     *
     * @param target the target status
     * @return true if transition is allowed
     */
    public boolean canTransitionTo(TaskStatus target) {
        return getAllowedTransitions().contains(target);
    }

    /**
     * Checks if this status is a terminal (final) state.
     *
     * @return true if this is a terminal state
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * Checks if this status allows editing.
     *
     * @return true if task can be edited
     */
    public boolean allowsEditing() {
        return this == PENDING_ACCEPT || this == IN_PROGRESS || this == REJECTED;
    }

    /**
     * Converts from legacy integer status.
     *
     * @param status legacy status integer
     * @return corresponding TaskStatus
     */
    public static TaskStatus fromLegacy(Integer status) {
        if (status == null) return PENDING_ACCEPT;
        return switch (status) {
            case 0 -> PENDING_ACCEPT;
            case 1 -> IN_PROGRESS;
            case 2 -> SUBMITTED;
            case 3 -> COMPLETED;
            case 4 -> REJECTED;
            default -> PENDING_ACCEPT;
        };
    }

    /**
     * Converts to legacy integer status.
     *
     * @return legacy status integer
     */
    public int toLegacy() {
        return switch (this) {
            case PENDING_ACCEPT -> 0;
            case IN_PROGRESS -> 1;
            case SUBMITTED -> 2;
            case APPROVED, COMPLETED -> 3;
            case REJECTED -> 4;
            case CANCELLED -> 5;
        };
    }
}
