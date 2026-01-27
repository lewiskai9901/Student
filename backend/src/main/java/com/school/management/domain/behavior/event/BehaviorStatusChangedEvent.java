package com.school.management.domain.behavior.event;

import com.school.management.domain.behavior.model.BehaviorRecord;
import com.school.management.domain.behavior.model.BehaviorStatus;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * Domain event raised when a behavior record status changes.
 */
public class BehaviorStatusChangedEvent extends BaseDomainEvent {

    private final Long behaviorRecordId;
    private final Long studentId;
    private final Long classId;
    private final BehaviorStatus previousStatus;
    private final BehaviorStatus newStatus;

    public BehaviorStatusChangedEvent(BehaviorRecord record, BehaviorStatus previousStatus,
                                       BehaviorStatus newStatus) {
        super("BehaviorRecord", String.valueOf(record.getId()));
        this.behaviorRecordId = record.getId();
        this.studentId = record.getStudentId();
        this.classId = record.getClassId();
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
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

    public BehaviorStatus getPreviousStatus() {
        return previousStatus;
    }

    public BehaviorStatus getNewStatus() {
        return newStatus;
    }
}
