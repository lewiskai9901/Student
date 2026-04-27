package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 申诉审核驳回事件 (P1#8).
 */
public class AppealRejectedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long appealId;
    private final String appealCode;
    private final Long reviewerId;
    private final String comment;

    public AppealRejectedEvent(Long appealId, String appealCode, Long reviewerId, String comment) {
        super("InspAppeal", appealId != null ? appealId.toString() : null);
        this.appealId = appealId;
        this.appealCode = appealCode;
        this.reviewerId = reviewerId;
        this.comment = comment;
    }

    public Long getAppealId() { return appealId; }
    public String getAppealCode() { return appealCode; }
    public Long getReviewerId() { return reviewerId; }
    public String getComment() { return comment; }
}
