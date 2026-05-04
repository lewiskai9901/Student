package com.school.management.infrastructure.extension.plugins.education.domain.teaching.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class GradeBatchPublishedEvent extends BaseDomainEvent {
    private final Long batchId;
    private final String batchName;
    private final Long semesterId;
    private final Long courseId;
    private final Long orgUnitId;
    private final Long publishedBy;

    public GradeBatchPublishedEvent(Long batchId, String batchName, Long semesterId,
                                    Long courseId, Long orgUnitId, Long publishedBy) {
        super("GradeBatch", batchId != null ? batchId.toString() : null);
        this.batchId = batchId;
        this.batchName = batchName;
        this.semesterId = semesterId;
        this.courseId = courseId;
        this.orgUnitId = orgUnitId;
        this.publishedBy = publishedBy;
    }

    public GradeBatchPublishedEvent(Long batchId, String batchName, Long semesterId) {
        this(batchId, batchName, semesterId, null, null, null);
    }

    public Long getBatchId() { return batchId; }
    public String getBatchName() { return batchName; }
    public Long getSemesterId() { return semesterId; }
    public Long getCourseId() { return courseId; }
    public Long getOrgUnitId() { return orgUnitId; }
    public Long getPublishedBy() { return publishedBy; }
}
