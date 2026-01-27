package com.school.management.domain.inspection.model;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a bonus score record within a class inspection.
 */
public class InspectionBonus extends Entity<Long> {

    private Long id;
    private Long classRecordId;
    private Long sessionId;
    private Long classId;
    private Long bonusItemId;
    private BigDecimal bonusScore;
    private String reason;
    private Long recordedBy;
    private LocalDateTime createdAt;

    protected InspectionBonus() {
    }

    private InspectionBonus(Builder builder) {
        this.id = builder.id;
        this.classRecordId = builder.classRecordId;
        this.sessionId = builder.sessionId;
        this.classId = builder.classId;
        this.bonusItemId = builder.bonusItemId;
        this.bonusScore = builder.bonusScore != null ? builder.bonusScore : BigDecimal.ZERO;
        this.reason = builder.reason;
        this.recordedBy = builder.recordedBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClassRecordId() { return classRecordId; }
    public Long getSessionId() { return sessionId; }
    public Long getClassId() { return classId; }
    public Long getBonusItemId() { return bonusItemId; }
    public BigDecimal getBonusScore() { return bonusScore; }
    public String getReason() { return reason; }
    public Long getRecordedBy() { return recordedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long classRecordId;
        private Long sessionId;
        private Long classId;
        private Long bonusItemId;
        private BigDecimal bonusScore;
        private String reason;
        private Long recordedBy;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder classRecordId(Long classRecordId) { this.classRecordId = classRecordId; return this; }
        public Builder sessionId(Long sessionId) { this.sessionId = sessionId; return this; }
        public Builder classId(Long classId) { this.classId = classId; return this; }
        public Builder bonusItemId(Long bonusItemId) { this.bonusItemId = bonusItemId; return this; }
        public Builder bonusScore(BigDecimal bonusScore) { this.bonusScore = bonusScore; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder recordedBy(Long recordedBy) { this.recordedBy = recordedBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public InspectionBonus build() {
            return new InspectionBonus(this);
        }
    }
}
