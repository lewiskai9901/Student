package com.school.management.domain.inspection.event;

import com.school.management.domain.inspection.model.InspectionRecord;
import com.school.management.domain.shared.event.BaseDomainEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain event raised when an inspection record is published.
 */
public class InspectionRecordPublishedEvent extends BaseDomainEvent {

    private final Long recordId;
    private final String recordCode;
    private final Long templateId;
    private final LocalDate inspectionDate;
    private final String inspectionPeriod;
    private final int classCount;
    private final int totalDeductions;
    private final double averageScore;
    private final LocalDateTime publishedAt;

    public InspectionRecordPublishedEvent(InspectionRecord record) {
        super("InspectionRecord", String.valueOf(record.getId()));
        this.recordId = record.getId();
        this.recordCode = record.getRecordCode();
        this.templateId = record.getTemplateId();
        this.inspectionDate = record.getInspectionDate();
        this.inspectionPeriod = record.getInspectionPeriod();
        this.classCount = record.getClassScores().size();
        this.totalDeductions = record.getTotalDeductionCount();
        this.averageScore = record.calculateAverageScore();
        this.publishedAt = record.getPublishedAt();
    }

    public Long getRecordId() {
        return recordId;
    }

    public String getRecordCode() {
        return recordCode;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public LocalDate getInspectionDate() {
        return inspectionDate;
    }

    public String getInspectionPeriod() {
        return inspectionPeriod;
    }

    public int getClassCount() {
        return classCount;
    }

    public int getTotalDeductions() {
        return totalDeductions;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}
