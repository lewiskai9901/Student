package com.school.management.domain.inspection.model;

import com.school.management.domain.inspection.event.InspectionRecordPublishedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * InspectionRecord Aggregate Root.
 * Represents a single inspection round with all class scores and deduction details.
 */
public class InspectionRecord extends AggregateRoot<Long> {

    private Long id;
    private String recordCode;
    private Long templateId;
    private Integer templateVersion;
    private Long roundId;
    private LocalDate inspectionDate;
    private String inspectionPeriod;
    private RecordStatus status;
    private Long inspectorId;
    private String inspectorName;
    private LocalDateTime inspectedAt;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    private LocalDateTime publishedAt;
    private String remarks;
    private LocalDateTime createdAt;
    private Long createdBy;

    private List<ClassScore> classScores;

    // For JPA/MyBatis
    protected InspectionRecord() {
        this.classScores = new ArrayList<>();
    }

    private InspectionRecord(Builder builder) {
        this.id = builder.id;
        this.recordCode = builder.recordCode;
        this.templateId = builder.templateId;
        this.templateVersion = builder.templateVersion;
        this.roundId = builder.roundId;
        this.inspectionDate = builder.inspectionDate;
        this.inspectionPeriod = builder.inspectionPeriod;
        this.status = RecordStatus.DRAFT;
        this.inspectorId = builder.inspectorId;
        this.inspectorName = builder.inspectorName;
        this.remarks = builder.remarks;
        this.createdAt = LocalDateTime.now();
        this.createdBy = builder.createdBy;
        this.classScores = new ArrayList<>();

        validate();
    }

    /**
     * Factory method to create a new inspection record.
     */
    public static InspectionRecord create(String recordCode, Long templateId,
                                          Integer templateVersion, Long roundId,
                                          LocalDate inspectionDate, String inspectionPeriod,
                                          Long inspectorId, String inspectorName,
                                          Long createdBy) {
        return builder()
            .recordCode(recordCode)
            .templateId(templateId)
            .templateVersion(templateVersion)
            .roundId(roundId)
            .inspectionDate(inspectionDate)
            .inspectionPeriod(inspectionPeriod)
            .inspectorId(inspectorId)
            .inspectorName(inspectorName)
            .createdBy(createdBy)
            .build();
    }

    /**
     * Adds a class score to this record.
     */
    public ClassScore addClassScore(Long classId, String className, Integer baseScore) {
        if (status != RecordStatus.DRAFT) {
            throw new IllegalStateException("Cannot add scores to non-draft record");
        }

        ClassScore score = ClassScore.create(this.id, classId, className, baseScore);
        this.classScores.add(score);
        return score;
    }

    /**
     * Records a deduction for a specific class.
     */
    public void recordDeduction(Long classId, Long deductionItemId, String itemName,
                                int count, java.math.BigDecimal deductionAmount,
                                String remark, List<String> evidenceUrls) {
        if (status != RecordStatus.DRAFT) {
            throw new IllegalStateException("Cannot record deductions on non-draft record");
        }

        ClassScore classScore = findClassScore(classId)
            .orElseThrow(() -> new IllegalArgumentException("Class not found in this record: " + classId));

        classScore.addDeduction(deductionItemId, itemName, count, deductionAmount, remark, evidenceUrls);
    }

    /**
     * Submits the record for review.
     */
    public void submit() {
        if (status != RecordStatus.DRAFT) {
            throw new IllegalStateException("Only draft records can be submitted");
        }
        if (classScores.isEmpty()) {
            throw new IllegalStateException("Cannot submit record without any class scores");
        }

        this.status = RecordStatus.SUBMITTED;
        this.inspectedAt = LocalDateTime.now();

        // Calculate final scores for all classes
        classScores.forEach(ClassScore::calculateFinalScore);
    }

    /**
     * Approves the record after review.
     */
    public void approve(Long reviewerId) {
        if (status != RecordStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted records can be approved");
        }

        this.status = RecordStatus.APPROVED;
        this.reviewerId = reviewerId;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * Rejects the record back to draft.
     */
    public void reject(Long reviewerId, String reason) {
        if (status != RecordStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted records can be rejected");
        }

        this.status = RecordStatus.DRAFT;
        this.reviewerId = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.remarks = reason;
    }

    /**
     * Publishes the record, making scores effective.
     */
    public void publish() {
        if (status != RecordStatus.APPROVED) {
            throw new IllegalStateException("Only approved records can be published");
        }

        this.status = RecordStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();

        registerEvent(new InspectionRecordPublishedEvent(this));
    }

    /**
     * Voids a published record.
     */
    public void voidRecord(String reason) {
        if (status != RecordStatus.PUBLISHED) {
            throw new IllegalStateException("Only published records can be voided");
        }

        this.status = RecordStatus.VOIDED;
        this.remarks = reason;
    }

    /**
     * Finds a class score by class ID.
     */
    public Optional<ClassScore> findClassScore(Long classId) {
        return classScores.stream()
            .filter(s -> s.getClassId().equals(classId))
            .findFirst();
    }

    /**
     * Calculates the average score across all classes.
     */
    public double calculateAverageScore() {
        if (classScores.isEmpty()) {
            return 0.0;
        }
        return classScores.stream()
            .mapToDouble(s -> s.getFinalScore().doubleValue())
            .average()
            .orElse(0.0);
    }

    /**
     * Gets the total number of deductions across all classes.
     */
    public int getTotalDeductionCount() {
        return classScores.stream()
            .mapToInt(s -> s.getDeductionDetails().size())
            .sum();
    }

    private void validate() {
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

    public String getRecordCode() {
        return recordCode;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Integer getTemplateVersion() {
        return templateVersion;
    }

    public Long getRoundId() {
        return roundId;
    }

    public LocalDate getInspectionDate() {
        return inspectionDate;
    }

    public String getInspectionPeriod() {
        return inspectionPeriod;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public Long getInspectorId() {
        return inspectorId;
    }

    public String getInspectorName() {
        return inspectorName;
    }

    public LocalDateTime getInspectedAt() {
        return inspectedAt;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
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

    public List<ClassScore> getClassScores() {
        return Collections.unmodifiableList(classScores);
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String recordCode;
        private Long templateId;
        private Integer templateVersion;
        private Long roundId;
        private LocalDate inspectionDate;
        private String inspectionPeriod;
        private Long inspectorId;
        private String inspectorName;
        private String remarks;
        private Long createdBy;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder recordCode(String recordCode) {
            this.recordCode = recordCode;
            return this;
        }

        public Builder templateId(Long templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder templateVersion(Integer templateVersion) {
            this.templateVersion = templateVersion;
            return this;
        }

        public Builder roundId(Long roundId) {
            this.roundId = roundId;
            return this;
        }

        public Builder inspectionDate(LocalDate inspectionDate) {
            this.inspectionDate = inspectionDate;
            return this;
        }

        public Builder inspectionPeriod(String inspectionPeriod) {
            this.inspectionPeriod = inspectionPeriod;
            return this;
        }

        public Builder inspectorId(Long inspectorId) {
            this.inspectorId = inspectorId;
            return this;
        }

        public Builder inspectorName(String inspectorName) {
            this.inspectorName = inspectorName;
            return this;
        }

        public Builder remarks(String remarks) {
            this.remarks = remarks;
            return this;
        }

        public Builder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public InspectionRecord build() {
            return new InspectionRecord(this);
        }
    }
}
