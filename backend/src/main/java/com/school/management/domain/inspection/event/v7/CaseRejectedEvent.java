package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class CaseRejectedEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long caseId;
    private final String caseCode;
    private final Long verifierId;
    private final String reason;

    public CaseRejectedEvent(Long caseId, String caseCode,
                             Long verifierId, String reason) {
        super("CorrectiveCase", caseId != null ? caseId.toString() : null);
        this.caseId = caseId;
        this.caseCode = caseCode;
        this.verifierId = verifierId;
        this.reason = reason;
    }

    public Long getCaseId() { return caseId; }
    public String getCaseCode() { return caseCode; }
    public Long getVerifierId() { return verifierId; }
    public String getReason() { return reason; }
}
