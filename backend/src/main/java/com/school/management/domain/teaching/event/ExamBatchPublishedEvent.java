package com.school.management.domain.teaching.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class ExamBatchPublishedEvent extends BaseDomainEvent {
    private final Long batchId;
    private final String batchName;
    private final Long semesterId;

    public ExamBatchPublishedEvent(Long batchId, String batchName, Long semesterId) {
        super("ExamBatch", batchId != null ? batchId.toString() : null);
        this.batchId = batchId;
        this.batchName = batchName;
        this.semesterId = semesterId;
    }

    public Long getBatchId() { return batchId; }
    public String getBatchName() { return batchName; }
    public Long getSemesterId() { return semesterId; }
}
