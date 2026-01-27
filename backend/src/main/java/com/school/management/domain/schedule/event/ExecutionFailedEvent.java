package com.school.management.domain.schedule.event;

import com.school.management.domain.schedule.model.ScheduleExecution;
import com.school.management.domain.shared.event.BaseDomainEvent;

import java.time.LocalDate;

/**
 * Domain event raised when a schedule execution fails.
 */
public class ExecutionFailedEvent extends BaseDomainEvent {

    private final Long executionId;
    private final Long policyId;
    private final LocalDate executionDate;
    private final String failureReason;

    public ExecutionFailedEvent(ScheduleExecution execution) {
        super("ScheduleExecution", execution.getId());
        this.executionId = execution.getId();
        this.policyId = execution.getPolicyId();
        this.executionDate = execution.getExecutionDate();
        this.failureReason = execution.getFailureReason();
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

    public String getFailureReason() {
        return failureReason;
    }
}
