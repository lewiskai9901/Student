package com.school.management.interfaces.rest.inspection.dto;

import com.school.management.domain.inspection.model.InspectionSession;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SessionResponse {

    private Long id;
    private String sessionCode;
    private Long templateId;
    private Integer templateVersion;
    private LocalDate inspectionDate;
    private String inspectionPeriod;
    private String inputMode;
    private String scoringMode;
    private Integer baseScore;
    private String inspectionLevel;
    private String status;
    private Long inspectorId;
    private String inspectorName;
    private LocalDateTime submittedAt;
    private LocalDateTime publishedAt;
    private String remarks;
    private LocalDateTime createdAt;

    public static SessionResponse fromDomain(InspectionSession session) {
        SessionResponse r = new SessionResponse();
        r.setId(session.getId());
        r.setSessionCode(session.getSessionCode());
        r.setTemplateId(session.getTemplateId());
        r.setTemplateVersion(session.getTemplateVersion());
        r.setInspectionDate(session.getInspectionDate());
        r.setInspectionPeriod(session.getInspectionPeriod());
        r.setInputMode(session.getInputMode().name());
        r.setScoringMode(session.getScoringMode().name());
        r.setBaseScore(session.getBaseScore());
        r.setInspectionLevel(session.getInspectionLevel() != null ? session.getInspectionLevel().name() : "CLASS");
        r.setStatus(session.getStatus().name());
        r.setInspectorId(session.getInspectorId());
        r.setInspectorName(session.getInspectorName());
        r.setSubmittedAt(session.getSubmittedAt());
        r.setPublishedAt(session.getPublishedAt());
        r.setRemarks(session.getRemarks());
        r.setCreatedAt(session.getCreatedAt());
        return r;
    }
}
