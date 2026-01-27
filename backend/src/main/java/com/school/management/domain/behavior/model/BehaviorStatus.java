package com.school.management.domain.behavior.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Behavior record status with state machine transitions.
 * Implements the 4-state behavior tracking workflow.
 */
public enum BehaviorStatus {

    /**
     * Initial state - behavior just recorded
     */
    RECORDED("已记录", Collections.emptyList()),

    /**
     * Student/class has been notified
     */
    NOTIFIED("已通知", Collections.emptyList()),

    /**
     * Student/class has acknowledged the behavior record
     */
    ACKNOWLEDGED("已确认", Collections.emptyList()),

    /**
     * Behavior has been resolved/handled
     */
    RESOLVED("已处理", Collections.emptyList());

    private final String displayName;
    private List<BehaviorStatus> allowedTransitions;

    BehaviorStatus(String displayName, List<BehaviorStatus> allowedTransitions) {
        this.displayName = displayName;
        this.allowedTransitions = allowedTransitions;
    }

    // Initialize transitions after all enum values are created
    static {
        RECORDED.allowedTransitions = Arrays.asList(NOTIFIED);
        NOTIFIED.allowedTransitions = Arrays.asList(ACKNOWLEDGED);
        ACKNOWLEDGED.allowedTransitions = Arrays.asList(RESOLVED);
        RESOLVED.allowedTransitions = Collections.emptyList();
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if transition to the target status is allowed.
     */
    public boolean canTransitionTo(BehaviorStatus target) {
        return allowedTransitions.contains(target);
    }

    /**
     * Returns all allowed next statuses from current status.
     */
    public List<BehaviorStatus> getAllowedTransitions() {
        return Collections.unmodifiableList(allowedTransitions);
    }

    /**
     * Checks if this is a terminal (final) state.
     */
    public boolean isTerminal() {
        return allowedTransitions.isEmpty();
    }
}
