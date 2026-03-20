package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

import java.time.LocalDateTime;

public class SlaBreachedEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long caseId;
    private final String caseCode;
    private final Integer escalationLevel;
    private final LocalDateTime deadline;

    public SlaBreachedEvent(Long caseId, String caseCode, Integer escalationLevel, LocalDateTime deadline) {
        super("CorrectiveCase", caseId != null ? caseId.toString() : null);
        this.caseId = caseId;
        this.caseCode = caseCode;
        this.escalationLevel = escalationLevel;
        this.deadline = deadline;
    }

    public Long getCaseId() { return caseId; }
    public String getCaseCode() { return caseCode; }
    public Integer getEscalationLevel() { return escalationLevel; }
    public LocalDateTime getDeadline() { return deadline; }
}
