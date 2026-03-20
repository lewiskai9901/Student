package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class CorrectiveCaseCreatedEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long caseId;
    private final String caseCode;
    private final Long submissionId;
    private final Long detailId;
    private final String priority;

    public CorrectiveCaseCreatedEvent(Long caseId, String caseCode,
                                      Long submissionId, Long detailId, String priority) {
        super("CorrectiveCase", caseId != null ? caseId.toString() : null);
        this.caseId = caseId;
        this.caseCode = caseCode;
        this.submissionId = submissionId;
        this.detailId = detailId;
        this.priority = priority;
    }

    public Long getCaseId() { return caseId; }
    public String getCaseCode() { return caseCode; }
    public Long getSubmissionId() { return submissionId; }
    public Long getDetailId() { return detailId; }
    public String getPriority() { return priority; }
}
