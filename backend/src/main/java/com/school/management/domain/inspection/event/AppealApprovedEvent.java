package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

import java.math.BigDecimal;

/**
 * 申诉审核通过事件 (P1#8).
 * 监听器应据此调整原扣分明细并重算项目分数.
 */
public class AppealApprovedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long appealId;
    private final String appealCode;
    private final Long submissionDetailId;
    private final Long reviewerId;
    private final BigDecimal finalAdjustment;

    public AppealApprovedEvent(Long appealId, String appealCode, Long submissionDetailId,
                                Long reviewerId, BigDecimal finalAdjustment) {
        super("InspAppeal", appealId != null ? appealId.toString() : null);
        this.appealId = appealId;
        this.appealCode = appealCode;
        this.submissionDetailId = submissionDetailId;
        this.reviewerId = reviewerId;
        this.finalAdjustment = finalAdjustment;
    }

    public Long getAppealId() { return appealId; }
    public String getAppealCode() { return appealCode; }
    public Long getSubmissionDetailId() { return submissionDetailId; }
    public Long getReviewerId() { return reviewerId; }
    public BigDecimal getFinalAdjustment() { return finalAdjustment; }
}
