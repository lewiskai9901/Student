package com.school.management.domain.corrective.event;

import com.school.management.domain.corrective.model.ActionStatus;
import com.school.management.domain.corrective.model.CorrectiveAction;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * Domain event raised when a corrective action is verified.
 */
public class ActionVerifiedEvent extends BaseDomainEvent {

    private final Long actionId;
    private final String actionCode;
    private final Long classId;
    private final ActionStatus previousStatus;
    private final ActionStatus newStatus;
    private final Long verifierId;
    private final String result;

    public ActionVerifiedEvent(CorrectiveAction action, ActionStatus previousStatus,
                                ActionStatus newStatus, Long verifierId, String result) {
        super("CorrectiveAction", String.valueOf(action.getId()));
        this.actionId = action.getId();
        this.actionCode = action.getActionCode();
        this.classId = action.getClassId();
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.verifierId = verifierId;
        this.result = result;
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

    public Long getVerifierId() {
        return verifierId;
    }

    public String getResult() {
        return result;
    }

    /**
     * Checks if the verification passed.
     */
    public boolean isPassed() {
        return "PASS".equals(result);
    }

    /**
     * Checks if the verification failed.
     */
    public boolean isFailed() {
        return "FAIL".equals(result);
    }
}
