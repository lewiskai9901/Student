package com.school.management.domain.corrective.event;

import com.school.management.domain.corrective.model.ActionSeverity;
import com.school.management.domain.corrective.model.CorrectiveAction;
import com.school.management.domain.shared.event.BaseDomainEvent;

import java.time.LocalDateTime;

/**
 * Domain event raised when a corrective action becomes overdue.
 */
public class ActionOverdueEvent extends BaseDomainEvent {

    private final Long actionId;
    private final String actionCode;
    private final Long classId;
    private final Long assigneeId;
    private final ActionSeverity severity;
    private final LocalDateTime deadline;

    public ActionOverdueEvent(CorrectiveAction action) {
        super("CorrectiveAction", String.valueOf(action.getId()));
        this.actionId = action.getId();
        this.actionCode = action.getActionCode();
        this.classId = action.getClassId();
        this.assigneeId = action.getAssigneeId();
        this.severity = action.getSeverity();
        this.deadline = action.getDeadline();
    }

    public Long getActionId() {
        return actionId;
    }

    public String getActionCode() {
        return actionCode;
    }

    public Long getClassId() {
        return classId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public ActionSeverity getSeverity() {
        return severity;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }
}
