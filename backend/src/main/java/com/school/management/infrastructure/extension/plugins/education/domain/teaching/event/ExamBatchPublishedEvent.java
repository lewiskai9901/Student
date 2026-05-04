package com.school.management.infrastructure.extension.plugins.education.domain.teaching.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class ExamBatchPublishedEvent extends BaseDomainEvent {
    private final Long batchId;
    private final String batchName;
    private final Long semesterId;
    private final Integer examType;
    private final Long publishedBy;

    public ExamBatchPublishedEvent(Long batchId, String batchName, Long semesterId,
                                   Integer examType, Long publishedBy) {
        super("ExamBatch", batchId != null ? batchId.toString() : null);
        this.batchId = batchId;
        this.batchName = batchName;
        this.semesterId = semesterId;
        this.examType = examType;
        this.publishedBy = publishedBy;
    }

    /** 兼容旧构造 */
    public ExamBatchPublishedEvent(Long batchId, String batchName, Long semesterId) {
        this(batchId, batchName, semesterId, null, null);
    }

    public Long getBatchId() { return batchId; }
    public String getBatchName() { return batchName; }
    public Long getSemesterId() { return semesterId; }
    public Integer getExamType() { return examType; }
    public Long getPublishedBy() { return publishedBy; }
}
