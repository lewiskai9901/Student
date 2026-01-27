package com.school.management.domain.inspection.model;

import com.school.management.domain.shared.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ClassInspectionRecord Aggregate Root.
 * Represents a single class's inspection record within a session.
 * Contains deduction details and checklist responses for one class.
 */
public class ClassInspectionRecord extends AggregateRoot<Long> {

    private Long id;
    private Long sessionId;
    private Long classId;
    private String className;
    private Long orgUnitId;
    private String orgUnitName;
    private Integer baseScore;
    private BigDecimal totalDeduction;
    private BigDecimal bonusScore;
    private BigDecimal finalScore;
    private ClassRecordStatus status;
    private String remarks;
    private LocalDateTime createdAt;

    private List<InspectionDeduction> deductions;
    private List<ChecklistResponse> checklistResponses;

    protected ClassInspectionRecord() {
        this.deductions = new ArrayList<>();
        this.checklistResponses = new ArrayList<>();
        this.totalDeduction = BigDecimal.ZERO;
        this.bonusScore = BigDecimal.ZERO;
        this.finalScore = BigDecimal.ZERO;
    }

    private ClassInspectionRecord(Builder builder) {
        this.id = builder.id;
        this.sessionId = builder.sessionId;
        this.classId = builder.classId;
        this.className = builder.className;
        this.orgUnitId = builder.orgUnitId;
        this.orgUnitName = builder.orgUnitName;
        this.baseScore = builder.baseScore != null ? builder.baseScore : 100;
        this.totalDeduction = builder.totalDeduction != null ? builder.totalDeduction : BigDecimal.ZERO;
        this.bonusScore = builder.bonusScore != null ? builder.bonusScore : BigDecimal.ZERO;
        this.finalScore = builder.finalScore != null ? builder.finalScore : BigDecimal.ZERO;
        this.status = builder.status != null ? builder.status : ClassRecordStatus.PENDING;
        this.remarks = builder.remarks;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.deductions = builder.deductions != null ? new ArrayList<>(builder.deductions) : new ArrayList<>();
        this.checklistResponses = builder.checklistResponses != null ? new ArrayList<>(builder.checklistResponses) : new ArrayList<>();
    }

    /**
     * Factory method to create a new class inspection record.
     */
    public static ClassInspectionRecord create(Long sessionId, Long classId, String className,
                                                Long orgUnitId, String orgUnitName,
                                                Integer baseScore) {
        return builder()
            .sessionId(sessionId)
            .classId(classId)
            .className(className)
            .orgUnitId(orgUnitId)
            .orgUnitName(orgUnitName)
            .baseScore(baseScore)
            .build();
    }

    /**
     * Adds a deduction to this class record.
     */
    public InspectionDeduction addDeduction(InspectionDeduction deduction) {
        this.deductions.add(deduction);
        recalculateScores();
        if (this.status == ClassRecordStatus.PENDING) {
            this.status = ClassRecordStatus.RECORDING;
        }
        return deduction;
    }

    /**
     * Adds or updates a checklist response.
     */
    public void addOrUpdateChecklistResponse(ChecklistResponse response) {
        // Remove existing response for the same checklist item
        this.checklistResponses.removeIf(
            r -> r.getChecklistItemId().equals(response.getChecklistItemId()));
        this.checklistResponses.add(response);
        recalculateScores();
        if (this.status == ClassRecordStatus.PENDING) {
            this.status = ClassRecordStatus.RECORDING;
        }
    }

    /**
     * Marks this record as completed.
     */
    public void complete() {
        recalculateScores();
        this.status = ClassRecordStatus.COMPLETED;
    }

    /**
     * Recalculates totalDeduction and finalScore based on deductions and checklist auto-deductions.
     */
    public void recalculateScores() {
        BigDecimal deductionSum = deductions.stream()
            .map(InspectionDeduction::getDeductionAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal checklistDeductionSum = checklistResponses.stream()
            .map(ChecklistResponse::getAutoDeduction)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalDeduction = deductionSum.add(checklistDeductionSum);
        this.finalScore = new BigDecimal(this.baseScore)
            .subtract(this.totalDeduction)
            .add(this.bonusScore);
    }

    // Getters
    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public Long getClassId() { return classId; }
    public String getClassName() { return className; }
    public Long getOrgUnitId() { return orgUnitId; }
    public String getOrgUnitName() { return orgUnitName; }
    public Integer getBaseScore() { return baseScore; }
    public BigDecimal getTotalDeduction() { return totalDeduction; }
    public BigDecimal getBonusScore() { return bonusScore; }
    public BigDecimal getFinalScore() { return finalScore; }
    public ClassRecordStatus getStatus() { return status; }
    public String getRemarks() { return remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<InspectionDeduction> getDeductions() { return Collections.unmodifiableList(deductions); }
    public List<ChecklistResponse> getChecklistResponses() { return Collections.unmodifiableList(checklistResponses); }

    /**
     * Internal method for repository reconstruction of deductions.
     */
    public void loadDeductions(List<InspectionDeduction> deductions) {
        this.deductions = new ArrayList<>(deductions);
    }

    /**
     * Internal method for repository reconstruction of checklist responses.
     */
    public void loadChecklistResponses(List<ChecklistResponse> responses) {
        this.checklistResponses = new ArrayList<>(responses);
    }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long sessionId;
        private Long classId;
        private String className;
        private Long orgUnitId;
        private String orgUnitName;
        private Integer baseScore;
        private BigDecimal totalDeduction;
        private BigDecimal bonusScore;
        private BigDecimal finalScore;
        private ClassRecordStatus status;
        private String remarks;
        private LocalDateTime createdAt;
        private List<InspectionDeduction> deductions;
        private List<ChecklistResponse> checklistResponses;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder sessionId(Long sessionId) { this.sessionId = sessionId; return this; }
        public Builder classId(Long classId) { this.classId = classId; return this; }
        public Builder className(String className) { this.className = className; return this; }
        public Builder orgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; return this; }
        public Builder orgUnitName(String orgUnitName) { this.orgUnitName = orgUnitName; return this; }
        public Builder baseScore(Integer baseScore) { this.baseScore = baseScore; return this; }
        public Builder totalDeduction(BigDecimal totalDeduction) { this.totalDeduction = totalDeduction; return this; }
        public Builder bonusScore(BigDecimal bonusScore) { this.bonusScore = bonusScore; return this; }
        public Builder finalScore(BigDecimal finalScore) { this.finalScore = finalScore; return this; }
        public Builder status(ClassRecordStatus status) { this.status = status; return this; }
        public Builder remarks(String remarks) { this.remarks = remarks; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder deductions(List<InspectionDeduction> deductions) { this.deductions = deductions; return this; }
        public Builder checklistResponses(List<ChecklistResponse> checklistResponses) { this.checklistResponses = checklistResponses; return this; }

        public ClassInspectionRecord build() {
            return new ClassInspectionRecord(this);
        }
    }
}
