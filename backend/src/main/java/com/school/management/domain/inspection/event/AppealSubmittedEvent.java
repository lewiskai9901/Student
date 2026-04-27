package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 申诉提交事件 (P1#8).
 */
public class AppealSubmittedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long appealId;
    private final String appealCode;
    private final Long submissionDetailId;
    private final Long submitterUserId;

    public AppealSubmittedEvent(Long appealId, String appealCode,
                                 Long submissionDetailId, Long submitterUserId) {
        super("InspAppeal", appealId != null ? appealId.toString() : null);
        this.appealId = appealId;
        this.appealCode = appealCode;
        this.submissionDetailId = submissionDetailId;
        this.submitterUserId = submitterUserId;
    }

    public Long getAppealId() { return appealId; }
    public String getAppealCode() { return appealCode; }
    public Long getSubmissionDetailId() { return submissionDetailId; }
    public Long getSubmitterUserId() { return submitterUserId; }
}
