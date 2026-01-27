package com.school.management.domain.schedule.model;

import com.school.management.domain.schedule.event.ExecutionCompletedEvent;
import com.school.management.domain.schedule.event.ExecutionFailedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ScheduleExecution Aggregate Root.
 * Represents a single execution instance of a schedule policy for a specific date.
 */
public class ScheduleExecution extends AggregateRoot<Long> {

    private Long id;
    private Long policyId;
    private LocalDate executionDate;
    private List<Long> assignedInspectors;
    private Long sessionId;
    private ExecutionStatus status;
    private String failureReason;
    private LocalDateTime createdAt;

    // For JPA/MyBatis
    protected ScheduleExecution() {
        this.assignedInspectors = new ArrayList<>();
    }

    private ScheduleExecution(Builder builder) {
        this.id = builder.id;
        this.policyId = builder.policyId;
        this.executionDate = builder.executionDate;
        this.assignedInspectors = builder.assignedInspectors != null
            ? new ArrayList<>(builder.assignedInspectors) : new ArrayList<>();
        this.sessionId = builder.sessionId;
        this.status = ExecutionStatus.PLANNED;
        this.createdAt = LocalDateTime.now();
    }

    private ScheduleExecution(Reconstructor r) {
        this.id = r.id;
        this.policyId = r.policyId;
        this.executionDate = r.executionDate;
        this.assignedInspectors = r.assignedInspectors != null
            ? new ArrayList<>(r.assignedInspectors) : new ArrayList<>();
        this.sessionId = r.sessionId;
        this.status = r.status;
        this.failureReason = r.failureReason;
        this.createdAt = r.createdAt;
    }

    /**
     * Marks this execution as successfully executed.
     * Transitions from PLANNED to EXECUTED.
     */
    public void execute() {
        assertCanTransitionTo(ExecutionStatus.EXECUTED);
        this.status = ExecutionStatus.EXECUTED;

        registerEvent(new ExecutionCompletedEvent(this));
    }

    /**
     * Marks this execution as skipped.
     * Transitions from PLANNED to SKIPPED.
     */
    public void skip(String reason) {
        assertCanTransitionTo(ExecutionStatus.SKIPPED);
        this.status = ExecutionStatus.SKIPPED;
        this.failureReason = reason;
    }

    /**
     * Marks this execution as failed.
     * Transitions from PLANNED to FAILED.
     */
    public void fail(String reason) {
        assertCanTransitionTo(ExecutionStatus.FAILED);
        this.status = ExecutionStatus.FAILED;
        this.failureReason = reason;

        registerEvent(new ExecutionFailedEvent(this));
    }

    /**
     * Links this execution to an inspection session.
     */
    public void linkSession(Long sessionId) {
        this.sessionId = sessionId;
    }

    private void assertCanTransitionTo(ExecutionStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", status, target));
        }
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public LocalDate getExecutionDate() {
        return executionDate;
    }

    public List<Long> getAssignedInspectors() {
        return Collections.unmodifiableList(assignedInspectors);
    }

    public Long getSessionId() {
        return sessionId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Builder (for creation)
    public static Builder builder() {
        return new Builder();
    }

    // Reconstructor (for restoring from persistence)
    public static Reconstructor reconstruct() {
        return new Reconstructor();
    }

    public static class Builder {
        private Long id;
        private Long policyId;
        private LocalDate executionDate;
        private List<Long> assignedInspectors;
        private Long sessionId;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder policyId(Long policyId) {
            this.policyId = policyId;
            return this;
        }

        public Builder executionDate(LocalDate executionDate) {
            this.executionDate = executionDate;
            return this;
        }

        public Builder assignedInspectors(List<Long> assignedInspectors) {
            this.assignedInspectors = assignedInspectors;
            return this;
        }

        public Builder sessionId(Long sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public ScheduleExecution build() {
            return new ScheduleExecution(this);
        }
    }

    public static class Reconstructor {
        private Long id;
        private Long policyId;
        private LocalDate executionDate;
        private List<Long> assignedInspectors;
        private Long sessionId;
        private ExecutionStatus status;
        private String failureReason;
        private LocalDateTime createdAt;

        public Reconstructor id(Long id) { this.id = id; return this; }
        public Reconstructor policyId(Long policyId) { this.policyId = policyId; return this; }
        public Reconstructor executionDate(LocalDate executionDate) { this.executionDate = executionDate; return this; }
        public Reconstructor assignedInspectors(List<Long> assignedInspectors) { this.assignedInspectors = assignedInspectors; return this; }
        public Reconstructor sessionId(Long sessionId) { this.sessionId = sessionId; return this; }
        public Reconstructor status(ExecutionStatus status) { this.status = status; return this; }
        public Reconstructor failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public Reconstructor createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ScheduleExecution build() {
            return new ScheduleExecution(this);
        }
    }
}
