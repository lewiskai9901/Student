package com.school.management.domain.task.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Status for individual task assignees.
 *
 * <p>Each assignee in a batch task has their own status progression.
 */
public enum AssigneeStatus {

    /**
     * Waiting for assignee to accept the task.
     */
    PENDING_ACCEPT("待接收"),

    /**
     * Assignee has accepted and is working on the task.
     */
    IN_PROGRESS("进行中"),

    /**
     * Assignee has submitted work for review.
     */
    PENDING_REVIEW("待审核"),

    /**
     * Work was rejected, needs revision.
     */
    REJECTED("已打回"),

    /**
     * Work was approved and completed.
     */
    COMPLETED("已完成");

    private final String displayName;

    AssigneeStatus(String displayName) {
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
    public Set<AssigneeStatus> getAllowedTransitions() {
        return switch (this) {
            case PENDING_ACCEPT -> EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(PENDING_REVIEW);
            case PENDING_REVIEW -> EnumSet.of(COMPLETED, REJECTED);
            case REJECTED -> EnumSet.of(IN_PROGRESS);
            case COMPLETED -> EnumSet.noneOf(AssigneeStatus.class);
        };
    }

    /**
     * Checks if this status can transition to the target status.
     *
     * @param target the target status
     * @return true if transition is allowed
     */
    public boolean canTransitionTo(AssigneeStatus target) {
        return getAllowedTransitions().contains(target);
    }

    /**
     * Converts from legacy integer status.
     *
     * @param status legacy status integer
     * @return corresponding AssigneeStatus
     */
    public static AssigneeStatus fromLegacy(Integer status) {
        if (status == null) return PENDING_ACCEPT;
        return switch (status) {
            case 0 -> PENDING_ACCEPT;
            case 1 -> IN_PROGRESS;
            case 2 -> PENDING_REVIEW;
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
            case PENDING_REVIEW -> 2;
            case COMPLETED -> 3;
            case REJECTED -> 4;
        };
    }
}
