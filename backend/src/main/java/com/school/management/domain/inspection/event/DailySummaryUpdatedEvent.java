package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

import java.time.LocalDate;

public class DailySummaryUpdatedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long projectId;
    private final LocalDate summaryDate;
    private final String targetType;
    private final Long targetId;

    public DailySummaryUpdatedEvent(Long projectId, LocalDate summaryDate,
                                     String targetType, Long targetId) {
        super("DailySummary", projectId != null ? projectId.toString() : null);
        this.projectId = projectId;
        this.summaryDate = summaryDate;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public Long getProjectId() { return projectId; }
    public LocalDate getSummaryDate() { return summaryDate; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
}
