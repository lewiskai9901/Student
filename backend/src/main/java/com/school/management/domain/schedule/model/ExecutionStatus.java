package com.school.management.domain.schedule.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Schedule execution status with state machine transitions.
 * Implements the execution lifecycle workflow.
 */
public enum ExecutionStatus {

    /**
     * Initial state - execution is planned but not yet run
     */
    PLANNED("已计划", Collections.emptyList()),

    /**
     * Execution completed successfully
     */
    EXECUTED("已执行", Collections.emptyList()),

    /**
     * Execution was skipped (e.g., holiday, excluded date)
     */
    SKIPPED("已跳过", Collections.emptyList()),

    /**
     * Execution failed due to an error
     */
    FAILED("失败", Collections.emptyList());

    private final String label;
    private List<ExecutionStatus> allowedTransitions;

    ExecutionStatus(String label, List<ExecutionStatus> allowedTransitions) {
        this.label = label;
        this.allowedTransitions = allowedTransitions;
    }

    // Initialize transitions after all enum values are created
    static {
        PLANNED.allowedTransitions = Arrays.asList(EXECUTED, SKIPPED, FAILED);
        EXECUTED.allowedTransitions = Collections.emptyList();
        SKIPPED.allowedTransitions = Collections.emptyList();
        FAILED.allowedTransitions = Arrays.asList(PLANNED);
    }

    public String getLabel() {
        return label;
    }

    /**
     * Checks if transition to the target status is allowed.
     */
    public boolean canTransitionTo(ExecutionStatus target) {
        return allowedTransitions.contains(target);
    }

    /**
     * Returns all allowed next statuses from current status.
     */
    public List<ExecutionStatus> getAllowedTransitions() {
        return Collections.unmodifiableList(allowedTransitions);
    }

    /**
     * Checks if this is a terminal (final) state.
     */
    public boolean isTerminal() {
        return allowedTransitions.isEmpty();
    }
}
