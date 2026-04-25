package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

import java.time.LocalDate;

public class CaseClosedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long caseId;
    private final String caseCode;
    private final LocalDate effectivenessCheckDate;

    public CaseClosedEvent(Long caseId, String caseCode, LocalDate effectivenessCheckDate) {
        super("CorrectiveCase", caseId != null ? caseId.toString() : null);
        this.caseId = caseId;
        this.caseCode = caseCode;
        this.effectivenessCheckDate = effectivenessCheckDate;
    }

    public Long getCaseId() { return caseId; }
    public String getCaseCode() { return caseCode; }
    public LocalDate getEffectivenessCheckDate() { return effectivenessCheckDate; }
}
