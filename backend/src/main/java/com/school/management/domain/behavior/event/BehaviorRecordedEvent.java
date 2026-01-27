package com.school.management.domain.behavior.event;

import com.school.management.domain.behavior.model.BehaviorCategory;
import com.school.management.domain.behavior.model.BehaviorRecord;
import com.school.management.domain.behavior.model.BehaviorSource;
import com.school.management.domain.behavior.model.BehaviorType;
import com.school.management.domain.shared.event.BaseDomainEvent;

import java.math.BigDecimal;

/**
 * Domain event raised when a new behavior record is created.
 */
public class BehaviorRecordedEvent extends BaseDomainEvent {

    private final Long behaviorRecordId;
    private final Long studentId;
    private final Long classId;
    private final BehaviorType behaviorType;
    private final BehaviorSource source;
    private final BehaviorCategory category;
    private final String title;
    private final BigDecimal deductionAmount;
    private final Long recordedBy;

    public BehaviorRecordedEvent(BehaviorRecord record) {
        super("BehaviorRecord", String.valueOf(record.getId()));
        this.behaviorRecordId = record.getId();
        this.studentId = record.getStudentId();
        this.classId = record.getClassId();
        this.behaviorType = record.getBehaviorType();
        this.source = record.getSource();
        this.category = record.getCategory();
        this.title = record.getTitle();
        this.deductionAmount = record.getDeductionAmount();
        this.recordedBy = record.getRecordedBy();
    }

    public Long getBehaviorRecordId() {
        return behaviorRecordId;
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

    public BehaviorCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getDeductionAmount() {
        return deductionAmount;
    }

    public Long getRecordedBy() {
        return recordedBy;
    }

    /**
     * Checks if this is a violation record.
     */
    public boolean isViolation() {
        return behaviorType == BehaviorType.VIOLATION;
    }
}
