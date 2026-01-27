package com.school.management.domain.corrective.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Corrective action status with state machine transitions.
 * Implements the corrective action workflow lifecycle.
 */
public enum ActionStatus {

    /**
     * Initial state - action just created
     */
    OPEN("Open", Collections.emptyList()),

    /**
     * Action is being worked on
     */
    IN_PROGRESS("In Progress", Collections.emptyList()),

    /**
     * Action submitted for review
     */
    REVIEW("Under Review", Collections.emptyList()),

    /**
     * Action completed and verified
     */
    CLOSED("Closed", Collections.emptyList()),

    /**
     * Action past its deadline
     */
    OVERDUE("Overdue", Collections.emptyList()),

    /**
     * Action escalated to higher authority
     */
    ESCALATED("Escalated", Collections.emptyList());

    private final String displayName;
    private List<ActionStatus> allowedTransitions;

    ActionStatus(String displayName, List<ActionStatus> allowedTransitions) {
        this.displayName = displayName;
        this.allowedTransitions = allowedTransitions;
    }

    // Initialize transitions after all enum values are created
    static {
        OPEN.allowedTransitions = Arrays.asList(IN_PROGRESS);
        IN_PROGRESS.allowedTransitions = Arrays.asList(REVIEW, OVERDUE);
        REVIEW.allowedTransitions = Arrays.asList(CLOSED, IN_PROGRESS);
        CLOSED.allowedTransitions = Collections.emptyList();
        OVERDUE.allowedTransitions = Arrays.asList(IN_PROGRESS, ESCALATED);
        ESCALATED.allowedTransitions = Arrays.asList(IN_PROGRESS);
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if transition to the target status is allowed.
     */
    public boolean canTransitionTo(ActionStatus target) {
        return allowedTransitions.contains(target);
    }

    /**
     * Returns all allowed next statuses from current status.
     */
    public List<ActionStatus> getAllowedTransitions() {
        return Collections.unmodifiableList(allowedTransitions);
    }

    /**
     * Checks if this is a terminal (final) state.
     */
    public boolean isTerminal() {
        return allowedTransitions.isEmpty();
    }

    /**
     * Checks if this status represents an open (non-closed) action.
     */
    public boolean isOpen() {
        return this != CLOSED;
    }
}
