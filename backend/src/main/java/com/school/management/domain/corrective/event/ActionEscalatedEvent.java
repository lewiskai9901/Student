package com.school.management.domain.corrective.event;

import com.school.management.domain.corrective.model.ActionSeverity;
import com.school.management.domain.corrective.model.CorrectiveAction;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * Domain event raised when a corrective action is escalated.
 */
public class ActionEscalatedEvent extends BaseDomainEvent {

    private final Long actionId;
    private final String actionCode;
    private final Long classId;
    private final Long assigneeId;
    private final ActionSeverity severity;
    private final int escalationLevel;

    public ActionEscalatedEvent(CorrectiveAction action, int escalationLevel) {
        super("CorrectiveAction", String.valueOf(action.getId()));
        this.actionId = action.getId();
        this.actionCode = action.getActionCode();
        this.classId = action.getClassId();
        this.assigneeId = action.getAssigneeId();
        this.severity = action.getSeverity();
        this.escalationLevel = escalationLevel;
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

    public int getEscalationLevel() {
        return escalationLevel;
    }
}
