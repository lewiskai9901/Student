package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain event raised when an inspection session is published.
 */
public class SessionPublishedEvent extends BaseDomainEvent {

    private final Long sessionId;
    private final String sessionCode;
    private final Long templateId;
    private final LocalDate inspectionDate;
    private final int classRecordCount;
    private final LocalDateTime publishedAt;

    public SessionPublishedEvent(Long sessionId, String sessionCode, Long templateId,
                                  LocalDate inspectionDate, int classRecordCount,
                                  LocalDateTime publishedAt) {
        super("InspectionSession", sessionId);
        this.sessionId = sessionId;
        this.sessionCode = sessionCode;
        this.templateId = templateId;
        this.inspectionDate = inspectionDate;
        this.classRecordCount = classRecordCount;
        this.publishedAt = publishedAt;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public LocalDate getInspectionDate() {
        return inspectionDate;
    }

    public int getClassRecordCount() {
        return classRecordCount;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}
