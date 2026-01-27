package com.school.management.domain.corrective.event;

import com.school.management.domain.corrective.model.ActionStatus;
import com.school.management.domain.corrective.model.CorrectiveAction;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * Domain event raised when a corrective action status changes.
 */
public class ActionStatusChangedEvent extends BaseDomainEvent {

    private final Long actionId;
    private final String actionCode;
    private final Long classId;
    private final ActionStatus previousStatus;
    private final ActionStatus newStatus;

    public ActionStatusChangedEvent(CorrectiveAction action, ActionStatus previousStatus,
                                     ActionStatus newStatus) {
        super("CorrectiveAction", String.valueOf(action.getId()));
        this.actionId = action.getId();
        this.actionCode = action.getActionCode();
        this.classId = action.getClassId();
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
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

    public ActionStatus getPreviousStatus() {
        return previousStatus;
    }

    public ActionStatus getNewStatus() {
        return newStatus;
    }
}
