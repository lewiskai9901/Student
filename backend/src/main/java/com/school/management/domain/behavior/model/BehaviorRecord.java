package com.school.management.domain.behavior.model;

import com.school.management.domain.behavior.event.BehaviorRecordedEvent;
import com.school.management.domain.behavior.event.BehaviorStatusChangedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BehaviorRecord Aggregate Root.
 * Represents a student behavior record (violation or commendation) with state machine workflow.
 */
public class BehaviorRecord extends AggregateRoot<Long> {

    private Long id;
    private Long studentId;
    private Long classId;
    private BehaviorType behaviorType;
    private BehaviorSource source;
    private Long sourceId;
    private BehaviorCategory category;
    private String title;
    private String detail;
    private BigDecimal deductionAmount;
    private BehaviorStatus status;
    private Long recordedBy;
    private LocalDateTime recordedAt;
    private LocalDateTime notifiedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private String resolutionNote;

    // For JPA/MyBatis
    protected BehaviorRecord() {
    }

    private BehaviorRecord(Builder builder) {
        this.id = builder.id;
        this.studentId = builder.studentId;
        this.classId = builder.classId;
        this.behaviorType = builder.behaviorType;
        this.source = builder.source;
        this.sourceId = builder.sourceId;
        this.category = builder.category;
        this.title = builder.title;
        this.detail = builder.detail;
        this.deductionAmount = builder.deductionAmount;
        this.status = builder.status;
        this.recordedBy = builder.recordedBy;
        this.recordedAt = builder.recordedAt;
        this.notifiedAt = builder.notifiedAt;
        this.acknowledgedAt = builder.acknowledgedAt;
        this.resolvedAt = builder.resolvedAt;
        this.resolutionNote = builder.resolutionNote;

        validate();
    }

    /**
     * Factory method to create a new violation record.
     */
    public static BehaviorRecord createViolation(Long studentId, Long classId,
                                                  BehaviorSource source, Long sourceId,
                                                  BehaviorCategory category, String title,
                                                  BigDecimal deductionAmount, Long recordedBy) {
        BehaviorRecord record = builder()
                .studentId(studentId)
                .classId(classId)
                .behaviorType(BehaviorType.VIOLATION)
                .source(source)
                .sourceId(sourceId)
                .category(category)
                .title(title)
                .deductionAmount(deductionAmount)
                .status(BehaviorStatus.RECORDED)
                .recordedBy(recordedBy)
                .recordedAt(LocalDateTime.now())
                .build();

        record.registerEvent(new BehaviorRecordedEvent(record));
        return record;
    }

    /**
     * Factory method to create a new commendation record.
     */
    public static BehaviorRecord createCommendation(Long studentId, Long classId,
                                                     BehaviorSource source, Long sourceId,
                                                     BehaviorCategory category, String title,
                                                     BigDecimal amount, Long recordedBy) {
        return builder()
                .studentId(studentId)
                .classId(classId)
                .behaviorType(BehaviorType.COMMENDATION)
                .source(source)
                .sourceId(sourceId)
                .category(category)
                .title(title)
                .deductionAmount(amount)
                .status(BehaviorStatus.RECORDED)
                .recordedBy(recordedBy)
                .recordedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Notifies the student/class about this behavior record.
     * Transition: RECORDED -> NOTIFIED
     */
    public void notify_() {
        assertCanTransitionTo(BehaviorStatus.NOTIFIED);
        BehaviorStatus oldStatus = this.status;
        this.status = BehaviorStatus.NOTIFIED;
        this.notifiedAt = LocalDateTime.now();

        registerEvent(new BehaviorStatusChangedEvent(this, oldStatus, this.status));
    }

    /**
     * Marks the behavior record as acknowledged.
     * Transition: NOTIFIED -> ACKNOWLEDGED
     */
    public void acknowledge() {
        assertCanTransitionTo(BehaviorStatus.ACKNOWLEDGED);
        this.status = BehaviorStatus.ACKNOWLEDGED;
        this.acknowledgedAt = LocalDateTime.now();
    }

    /**
     * Resolves the behavior record with a note.
     * Transition: ACKNOWLEDGED -> RESOLVED
     */
    public void resolve(String note) {
        assertCanTransitionTo(BehaviorStatus.RESOLVED);
        this.status = BehaviorStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNote = note;
    }

    private void assertCanTransitionTo(BehaviorStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", status, target));
        }
    }

    private void validate() {
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID is required");
        }
        if (classId == null) {
            throw new IllegalArgumentException("Class ID is required");
        }
        if (behaviorType == null) {
            throw new IllegalArgumentException("Behavior type is required");
        }
        if (source == null) {
            throw new IllegalArgumentException("Behavior source is required");
        }
        if (category == null) {
            throw new IllegalArgumentException("Behavior category is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (recordedBy == null) {
            throw new IllegalArgumentException("Recorder ID is required");
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

    public Long getStudentId() {
        return studentId;
    }

    public Long getClassId() {
        return classId;
    }

    public BehaviorType getBehaviorType() {
        return behaviorType;
    }

    public BehaviorSource getSource() {
        return source;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public BehaviorCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public BigDecimal getDeductionAmount() {
        return deductionAmount;
    }

    public BehaviorStatus getStatus() {
        return status;
    }

    public Long getRecordedBy() {
        return recordedBy;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Alias for builder(), used when reconstructing from persistence.
     */
    public static Builder reconstruct() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long studentId;
        private Long classId;
        private BehaviorType behaviorType;
        private BehaviorSource source;
        private Long sourceId;
        private BehaviorCategory category;
        private String title;
        private String detail;
        private BigDecimal deductionAmount;
        private BehaviorStatus status;
        private Long recordedBy;
        private LocalDateTime recordedAt;
        private LocalDateTime notifiedAt;
        private LocalDateTime acknowledgedAt;
        private LocalDateTime resolvedAt;
        private String resolutionNote;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder studentId(Long studentId) {
            this.studentId = studentId;
            return this;
        }

        public Builder classId(Long classId) {
            this.classId = classId;
            return this;
        }

        public Builder behaviorType(BehaviorType behaviorType) {
            this.behaviorType = behaviorType;
            return this;
        }

        public Builder source(BehaviorSource source) {
            this.source = source;
            return this;
        }

        public Builder sourceId(Long sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder category(BehaviorCategory category) {
            this.category = category;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder deductionAmount(BigDecimal deductionAmount) {
            this.deductionAmount = deductionAmount;
            return this;
        }

        public Builder status(BehaviorStatus status) {
            this.status = status;
            return this;
        }

        public Builder recordedBy(Long recordedBy) {
            this.recordedBy = recordedBy;
            return this;
        }

        public Builder recordedAt(LocalDateTime recordedAt) {
            this.recordedAt = recordedAt;
            return this;
        }

        public Builder notifiedAt(LocalDateTime notifiedAt) {
            this.notifiedAt = notifiedAt;
            return this;
        }

        public Builder acknowledgedAt(LocalDateTime acknowledgedAt) {
            this.acknowledgedAt = acknowledgedAt;
            return this;
        }

        public Builder resolvedAt(LocalDateTime resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public Builder resolutionNote(String resolutionNote) {
            this.resolutionNote = resolutionNote;
            return this;
        }

        public BehaviorRecord build() {
            return new BehaviorRecord(this);
        }
    }
}
