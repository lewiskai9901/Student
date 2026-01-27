package com.school.management.domain.inspection.model;

import com.school.management.domain.inspection.event.SessionPublishedEvent;
import com.school.management.domain.inspection.event.SessionSubmittedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * InspectionSession Aggregate Root.
 * Represents a single inspection session that groups multiple class inspection records.
 * Manages the lifecycle from creation through submission and publication.
 */
public class InspectionSession extends AggregateRoot<Long> {

    private Long id;
    private String sessionCode;
    private Long templateId;
    private Integer templateVersion;
    private LocalDate inspectionDate;
    private String inspectionPeriod;
    private InputMode inputMode;
    private ScoringMode scoringMode;
    private Integer baseScore;
    private SessionStatus status;
    private Long inspectorId;
    private String inspectorName;
    private LocalDateTime submittedAt;
    private LocalDateTime publishedAt;
    private String remarks;
    private LocalDateTime createdAt;
    private Long createdBy;

    // For MyBatis reconstruction
    protected InspectionSession() {
    }

    private InspectionSession(Builder builder) {
        this.id = builder.id;
        this.sessionCode = builder.sessionCode;
        this.templateId = builder.templateId;
        this.templateVersion = builder.templateVersion;
        this.inspectionDate = builder.inspectionDate;
        this.inspectionPeriod = builder.inspectionPeriod;
        this.inputMode = builder.inputMode != null ? builder.inputMode : InputMode.SPACE_FIRST;
        this.scoringMode = builder.scoringMode != null ? builder.scoringMode : ScoringMode.DEDUCTION_ONLY;
        this.baseScore = builder.baseScore != null ? builder.baseScore : 100;
        this.status = builder.status != null ? builder.status : SessionStatus.CREATED;
        this.inspectorId = builder.inspectorId;
        this.inspectorName = builder.inspectorName;
        this.submittedAt = builder.submittedAt;
        this.publishedAt = builder.publishedAt;
        this.remarks = builder.remarks;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.createdBy = builder.createdBy;

        if (builder.id == null) {
            validate();
        }
    }

    /**
     * Factory method to create a new inspection session.
     */
    public static InspectionSession create(String sessionCode, Long templateId,
                                            Integer templateVersion, LocalDate inspectionDate,
                                            String inspectionPeriod, InputMode inputMode,
                                            ScoringMode scoringMode, Integer baseScore,
                                            Long inspectorId, String inspectorName,
                                            Long createdBy) {
        return builder()
            .sessionCode(sessionCode)
            .templateId(templateId)
            .templateVersion(templateVersion)
            .inspectionDate(inspectionDate)
            .inspectionPeriod(inspectionPeriod)
            .inputMode(inputMode)
            .scoringMode(scoringMode)
            .baseScore(baseScore)
            .inspectorId(inspectorId)
            .inspectorName(inspectorName)
            .createdBy(createdBy)
            .build();
    }

    /**
     * Starts the inspection session. Moves from CREATED to IN_PROGRESS.
     */
    public void startInspection() {
        if (status != SessionStatus.CREATED) {
            throw new IllegalStateException("Only created sessions can be started");
        }
        this.status = SessionStatus.IN_PROGRESS;
    }

    /**
     * Submits the session for review/publication.
     */
    public void submit(int classRecordCount) {
        if (status != SessionStatus.CREATED && status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only created or in-progress sessions can be submitted");
        }
        if (classRecordCount == 0) {
            throw new IllegalStateException("Cannot submit session without any class records");
        }

        this.status = SessionStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();

        registerEvent(new SessionSubmittedEvent(
            this.id, this.sessionCode, this.templateId,
            this.inspectionDate, classRecordCount, this.submittedAt
        ));
    }

    /**
     * Publishes the session, making all scores effective.
     */
    public void publish(int classRecordCount) {
        if (status != SessionStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted sessions can be published");
        }

        this.status = SessionStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();

        registerEvent(new SessionPublishedEvent(
            this.id, this.sessionCode, this.templateId,
            this.inspectionDate, classRecordCount, this.publishedAt
        ));
    }

    private void validate() {
        if (sessionCode == null || sessionCode.isBlank()) {
            throw new IllegalArgumentException("Session code is required");
        }
        if (templateId == null) {
            throw new IllegalArgumentException("Template ID is required");
        }
        if (inspectionDate == null) {
            throw new IllegalArgumentException("Inspection date is required");
        }
        if (inspectorId == null) {
            throw new IllegalArgumentException("Inspector ID is required");
        }
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Integer getTemplateVersion() {
        return templateVersion;
    }

    public LocalDate getInspectionDate() {
        return inspectionDate;
    }

    public String getInspectionPeriod() {
        return inspectionPeriod;
    }

    public InputMode getInputMode() {
        return inputMode;
    }

    public ScoringMode getScoringMode() {
        return scoringMode;
    }

    public Integer getBaseScore() {
        return baseScore;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public Long getInspectorId() {
        return inspectorId;
    }

    public String getInspectorName() {
        return inspectorName;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String sessionCode;
        private Long templateId;
        private Integer templateVersion;
        private LocalDate inspectionDate;
        private String inspectionPeriod;
        private InputMode inputMode;
        private ScoringMode scoringMode;
        private Integer baseScore;
        private SessionStatus status;
        private Long inspectorId;
        private String inspectorName;
        private LocalDateTime submittedAt;
        private LocalDateTime publishedAt;
        private String remarks;
        private LocalDateTime createdAt;
        private Long createdBy;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder sessionCode(String sessionCode) { this.sessionCode = sessionCode; return this; }
        public Builder templateId(Long templateId) { this.templateId = templateId; return this; }
        public Builder templateVersion(Integer templateVersion) { this.templateVersion = templateVersion; return this; }
        public Builder inspectionDate(LocalDate inspectionDate) { this.inspectionDate = inspectionDate; return this; }
        public Builder inspectionPeriod(String inspectionPeriod) { this.inspectionPeriod = inspectionPeriod; return this; }
        public Builder inputMode(InputMode inputMode) { this.inputMode = inputMode; return this; }
        public Builder scoringMode(ScoringMode scoringMode) { this.scoringMode = scoringMode; return this; }
        public Builder baseScore(Integer baseScore) { this.baseScore = baseScore; return this; }
        public Builder status(SessionStatus status) { this.status = status; return this; }
        public Builder inspectorId(Long inspectorId) { this.inspectorId = inspectorId; return this; }
        public Builder inspectorName(String inspectorName) { this.inspectorName = inspectorName; return this; }
        public Builder submittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; return this; }
        public Builder publishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; return this; }
        public Builder remarks(String remarks) { this.remarks = remarks; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }

        public InspectionSession build() {
            return new InspectionSession(this);
        }
    }
}
