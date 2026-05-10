package com.school.management.domain.inspection.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.school.management.domain.shared.event.BaseDomainEvent;

import java.time.LocalDateTime;

public class SlaBreachedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long caseId;
    private final String caseCode;
    private final Integer escalationLevel;
    private final LocalDateTime deadline;

    @JsonCreator
    public SlaBreachedEvent(@JsonProperty("caseId") Long caseId,
                            @JsonProperty("caseCode") String caseCode,
                            @JsonProperty("escalationLevel") Integer escalationLevel,
                            @JsonProperty("deadline") LocalDateTime deadline) {
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
