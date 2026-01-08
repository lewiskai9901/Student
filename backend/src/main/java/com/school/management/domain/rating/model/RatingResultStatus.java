package com.school.management.domain.rating.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Rating result status with state machine.
 *
 * <p>State machine:
 * <pre>
 *     DRAFT → PENDING_APPROVAL → APPROVED → PUBLISHED
 *                            ↘
 *                             REJECTED → DRAFT
 * </pre>
 */
public enum RatingResultStatus {

    /**
     * Initial state, calculation complete but not submitted.
     */
    DRAFT("草稿"),

    /**
     * Submitted for approval.
     */
    PENDING_APPROVAL("待审核"),

    /**
     * Approved by reviewer.
     */
    APPROVED("已审核"),

    /**
     * Rejected by reviewer.
     */
    REJECTED("已拒绝"),

    /**
     * Published and visible to users.
     */
    PUBLISHED("已发布"),

    /**
     * Previously published but revoked.
     */
    REVOKED("已撤销");

    private final String displayName;

    RatingResultStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets allowed transitions from this status.
     *
     * @return set of allowed next statuses
     */
    public Set<RatingResultStatus> getAllowedTransitions() {
        return switch (this) {
            case DRAFT -> EnumSet.of(PENDING_APPROVAL);
            case PENDING_APPROVAL -> EnumSet.of(APPROVED, REJECTED);
            case APPROVED -> EnumSet.of(PUBLISHED);
            case REJECTED -> EnumSet.of(DRAFT);
            case PUBLISHED -> EnumSet.of(REVOKED);
            case REVOKED -> EnumSet.of(DRAFT);
        };
    }

    /**
     * Checks if transition to target is allowed.
     *
     * @param target target status
     * @return true if allowed
     */
    public boolean canTransitionTo(RatingResultStatus target) {
        return getAllowedTransitions().contains(target);
    }

    /**
     * Checks if this is a terminal state.
     *
     * @return true if terminal
     */
    public boolean isTerminal() {
        return false; // All states can transition
    }

    /**
     * Checks if results are visible to users.
     *
     * @return true if visible
     */
    public boolean isPublic() {
        return this == PUBLISHED;
    }

    public static RatingResultStatus fromString(String value) {
        if (value == null) return DRAFT;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DRAFT;
        }
    }
}
