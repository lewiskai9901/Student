package com.school.management.domain.corrective.event;

import com.school.management.domain.corrective.model.ActionSeverity;
import com.school.management.domain.corrective.model.ActionSource;
import com.school.management.domain.corrective.model.CorrectiveAction;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * Domain event raised when a corrective action is created.
 */
public class ActionCreatedEvent extends BaseDomainEvent {

    private final Long actionId;
    private final String actionCode;
    private final String title;
    private final ActionSource source;
    private final Long sourceId;
    private final Long classId;
    private final Long assigneeId;
    private final ActionSeverity severity;
    private final Long createdBy;

    public ActionCreatedEvent(CorrectiveAction action) {
        super("CorrectiveAction", String.valueOf(action.getId()));
        this.actionId = action.getId();
        this.actionCode = action.getActionCode();
        this.title = action.getTitle();
        this.source = action.getSource();
        this.sourceId = action.getSourceId();
        this.classId = action.getClassId();
        this.assigneeId = action.getAssigneeId();
        this.severity = action.getSeverity();
        this.createdBy = action.getCreatedBy();
    }

    public Long getActionId() {
        return actionId;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getTitle() {
        return title;
    }

    public ActionSource getSource() {
        return source;
    }

    public Long getSourceId() {
        return sourceId;
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

    public Long getCreatedBy() {
        return createdBy;
    }
}
