package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class CaseAssignedEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long caseId;
    private final String caseCode;
    private final Long assigneeId;
    private final String assigneeName;

    public CaseAssignedEvent(Long caseId, String caseCode,
                             Long assigneeId, String assigneeName) {
        super("CorrectiveCase", caseId != null ? caseId.toString() : null);
        this.caseId = caseId;
        this.caseCode = caseCode;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
    }

    public Long getCaseId() { return caseId; }
    public String getCaseCode() { return caseCode; }
    public Long getAssigneeId() { return assigneeId; }
    public String getAssigneeName() { return assigneeName; }
}
