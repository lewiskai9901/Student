package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

import java.math.BigDecimal;

public class SubmissionCompletedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long submissionId;
    private final Long taskId;
    private final String targetType;
    private final Long targetId;
    private final BigDecimal finalScore;

    public SubmissionCompletedEvent(Long submissionId, Long taskId,
                                    String targetType, Long targetId, BigDecimal finalScore) {
        super("InspSubmission", submissionId);
        this.submissionId = submissionId;
        this.taskId = taskId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.finalScore = finalScore;
    }

    public Long getSubmissionId() { return submissionId; }
    public Long getTaskId() { return taskId; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public BigDecimal getFinalScore() { return finalScore; }
}
