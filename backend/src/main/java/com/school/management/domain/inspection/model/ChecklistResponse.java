package com.school.management.domain.inspection.model;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a checklist item response within a class inspection record.
 */
public class ChecklistResponse extends Entity<Long> {

    private Long id;
    private Long sessionId;
    private Long classRecordId;
    private Long checklistItemId;
    private String itemName;
    private String categoryName;
    private ChecklistResult result;
    private BigDecimal autoDeduction;
    private String inspectorNote;
    private LocalDateTime createdAt;

    protected ChecklistResponse() {
    }

    private ChecklistResponse(Builder builder) {
        this.id = builder.id;
        this.sessionId = builder.sessionId;
        this.classRecordId = builder.classRecordId;
        this.checklistItemId = builder.checklistItemId;
        this.itemName = builder.itemName;
        this.categoryName = builder.categoryName;
        this.result = builder.result != null ? builder.result : ChecklistResult.NA;
        this.autoDeduction = builder.autoDeduction != null ? builder.autoDeduction : BigDecimal.ZERO;
        this.inspectorNote = builder.inspectorNote;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    /**
     * Updates the result and recalculates auto-deduction.
     */
    public void updateResult(ChecklistResult newResult, BigDecimal deductionWhenFail) {
        this.result = newResult;
        if (newResult == ChecklistResult.FAIL && deductionWhenFail != null) {
            this.autoDeduction = deductionWhenFail;
        } else {
            this.autoDeduction = BigDecimal.ZERO;
        }
    }

    // Getters
    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public Long getClassRecordId() { return classRecordId; }
    public Long getChecklistItemId() { return checklistItemId; }
    public String getItemName() { return itemName; }
    public String getCategoryName() { return categoryName; }
    public ChecklistResult getResult() { return result; }
    public BigDecimal getAutoDeduction() { return autoDeduction; }
    public String getInspectorNote() { return inspectorNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long sessionId;
        private Long classRecordId;
        private Long checklistItemId;
        private String itemName;
        private String categoryName;
        private ChecklistResult result;
        private BigDecimal autoDeduction;
        private String inspectorNote;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder sessionId(Long sessionId) { this.sessionId = sessionId; return this; }
        public Builder classRecordId(Long classRecordId) { this.classRecordId = classRecordId; return this; }
        public Builder checklistItemId(Long checklistItemId) { this.checklistItemId = checklistItemId; return this; }
        public Builder itemName(String itemName) { this.itemName = itemName; return this; }
        public Builder categoryName(String categoryName) { this.categoryName = categoryName; return this; }
        public Builder result(ChecklistResult result) { this.result = result; return this; }
        public Builder autoDeduction(BigDecimal autoDeduction) { this.autoDeduction = autoDeduction; return this; }
        public Builder inspectorNote(String inspectorNote) { this.inspectorNote = inspectorNote; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ChecklistResponse build() {
            return new ChecklistResponse(this);
        }
    }
}
