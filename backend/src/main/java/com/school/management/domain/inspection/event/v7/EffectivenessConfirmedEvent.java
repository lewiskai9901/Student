package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class EffectivenessConfirmedEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long caseId;
    private final String caseCode;

    public EffectivenessConfirmedEvent(Long caseId, String caseCode) {
        super("CorrectiveCase", caseId != null ? caseId.toString() : null);
        this.caseId = caseId;
        this.caseCode = caseCode;
    }

    public Long getCaseId() { return caseId; }
    public String getCaseCode() { return caseCode; }
}
