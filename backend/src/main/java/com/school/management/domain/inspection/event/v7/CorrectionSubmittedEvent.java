package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class CorrectionSubmittedEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long caseId;
    private final String caseCode;
    private final Long assigneeId;

    public CorrectionSubmittedEvent(Long caseId, String caseCode, Long assigneeId) {
        super("CorrectiveCase", caseId != null ? caseId.toString() : null);
        this.caseId = caseId;
        this.caseCode = caseCode;
        this.assigneeId = assigneeId;
    }

    public Long getCaseId() { return caseId; }
    public String getCaseCode() { return caseCode; }
    public Long getAssigneeId() { return assigneeId; }
}
