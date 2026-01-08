package com.school.management.domain.inspection.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Appeal status with state machine transitions.
 * Implements the 9-state appeal workflow.
 */
public enum AppealStatus {

    /**
     * Initial state - appeal just submitted
     */
    PENDING("Pending", Collections.emptyList()),

    /**
     * Level 1 review in progress
     */
    LEVEL1_REVIEWING("Level 1 Reviewing", Collections.emptyList()),

    /**
     * Level 1 approved, waiting for Level 2
     */
    LEVEL1_APPROVED("Level 1 Approved", Collections.emptyList()),

    /**
     * Level 1 rejected
     */
    LEVEL1_REJECTED("Level 1 Rejected", Collections.emptyList()),

    /**
     * Level 2 review in progress
     */
    LEVEL2_REVIEWING("Level 2 Reviewing", Collections.emptyList()),

    /**
     * Final approval
     */
    APPROVED("Approved", Collections.emptyList()),

    /**
     * Final rejection
     */
    REJECTED("Rejected", Collections.emptyList()),

    /**
     * Withdrawn by applicant
     */
    WITHDRAWN("Withdrawn", Collections.emptyList()),

    /**
     * Score adjusted and effective
     */
    EFFECTIVE("Effective", Collections.emptyList());

    private final String displayName;
    private List<AppealStatus> allowedTransitions;

    AppealStatus(String displayName, List<AppealStatus> allowedTransitions) {
        this.displayName = displayName;
        this.allowedTransitions = allowedTransitions;
    }

    // Initialize transitions after all enum values are created
    static {
        PENDING.allowedTransitions = Arrays.asList(LEVEL1_REVIEWING, WITHDRAWN);
        LEVEL1_REVIEWING.allowedTransitions = Arrays.asList(LEVEL1_APPROVED, LEVEL1_REJECTED);
        LEVEL1_APPROVED.allowedTransitions = Arrays.asList(LEVEL2_REVIEWING);
        LEVEL1_REJECTED.allowedTransitions = Collections.emptyList();
        LEVEL2_REVIEWING.allowedTransitions = Arrays.asList(APPROVED, REJECTED);
        APPROVED.allowedTransitions = Arrays.asList(EFFECTIVE);
        REJECTED.allowedTransitions = Collections.emptyList();
        WITHDRAWN.allowedTransitions = Collections.emptyList();
        EFFECTIVE.allowedTransitions = Collections.emptyList();
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if transition to the target status is allowed.
     */
    public boolean canTransitionTo(AppealStatus target) {
        return allowedTransitions.contains(target);
    }

    /**
     * Returns all allowed next statuses from current status.
     */
    public List<AppealStatus> getAllowedTransitions() {
        return Collections.unmodifiableList(allowedTransitions);
    }

    /**
     * Checks if this is a terminal (final) state.
     */
    public boolean isTerminal() {
        return allowedTransitions.isEmpty();
    }

    /**
     * Checks if this status requires reviewer action.
     */
    public boolean requiresReviewerAction() {
        return this == LEVEL1_REVIEWING || this == LEVEL2_REVIEWING;
    }

    /**
     * Checks if the appeal was ultimately successful.
     */
    public boolean isSuccessful() {
        return this == APPROVED || this == EFFECTIVE;
    }
}
