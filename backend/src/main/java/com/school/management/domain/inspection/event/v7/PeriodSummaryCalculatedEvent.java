package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

import java.time.LocalDate;

public class PeriodSummaryCalculatedEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long projectId;
    private final String periodType;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;

    public PeriodSummaryCalculatedEvent(Long projectId, String periodType,
                                         LocalDate periodStart, LocalDate periodEnd) {
        super("PeriodSummary", projectId != null ? projectId.toString() : null);
        this.projectId = projectId;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public Long getProjectId() { return projectId; }
    public String getPeriodType() { return periodType; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
}
