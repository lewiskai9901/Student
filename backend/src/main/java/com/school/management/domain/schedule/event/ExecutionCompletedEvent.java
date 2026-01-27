package com.school.management.domain.schedule.event;

import com.school.management.domain.schedule.model.ScheduleExecution;
import com.school.management.domain.shared.event.BaseDomainEvent;

import java.time.LocalDate;
import java.util.List;

/**
 * Domain event raised when a schedule execution completes successfully.
 */
public class ExecutionCompletedEvent extends BaseDomainEvent {

    private final Long executionId;
    private final Long policyId;
    private final LocalDate executionDate;
    private final List<Long> assignedInspectors;
    private final Long sessionId;

    public ExecutionCompletedEvent(ScheduleExecution execution) {
        super("ScheduleExecution", execution.getId());
        this.executionId = execution.getId();
        this.policyId = execution.getPolicyId();
        this.executionDate = execution.getExecutionDate();
        this.assignedInspectors = execution.getAssignedInspectors();
        this.sessionId = execution.getSessionId();
    }

    public Long getExecutionId() {
        return executionId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public LocalDate getExecutionDate() {
        return executionDate;
    }

    public List<Long> getAssignedInspectors() {
        return assignedInspectors;
    }

    public Long getSessionId() {
        return sessionId;
    }
}
