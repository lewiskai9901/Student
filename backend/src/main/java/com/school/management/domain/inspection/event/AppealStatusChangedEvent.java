package com.school.management.domain.inspection.event;

import com.school.management.domain.inspection.model.Appeal;
import com.school.management.domain.inspection.model.AppealStatus;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * Domain event raised when an appeal status changes.
 */
public class AppealStatusChangedEvent extends BaseDomainEvent {

    private final Long appealId;
    private final String appealCode;
    private final Long classId;
    private final AppealStatus previousStatus;
    private final AppealStatus newStatus;
    private final Long changedBy;

    public AppealStatusChangedEvent(Appeal appeal, AppealStatus previousStatus,
                                    AppealStatus newStatus, Long changedBy) {
        super("Appeal", String.valueOf(appeal.getId()));
        this.appealId = appeal.getId();
        this.appealCode = appeal.getAppealCode();
        this.classId = appeal.getClassId();
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
    }

    public Long getAppealId() {
        return appealId;
    }

    public String getAppealCode() {
        return appealCode;
    }

    public Long getClassId() {
        return classId;
    }

    public AppealStatus getPreviousStatus() {
        return previousStatus;
    }

    public AppealStatus getNewStatus() {
        return newStatus;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    /**
     * Checks if the appeal was approved (reached final approval state).
     */
    public boolean isApproved() {
        return newStatus == AppealStatus.APPROVED;
    }

    /**
     * Checks if the appeal was rejected (reached final rejection state).
     */
    public boolean isRejected() {
        return newStatus == AppealStatus.REJECTED || newStatus == AppealStatus.LEVEL1_REJECTED;
    }
}
