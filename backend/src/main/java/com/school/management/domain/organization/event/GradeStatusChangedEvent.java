package com.school.management.domain.organization.event;

import com.school.management.domain.organization.model.Grade;
import com.school.management.domain.organization.model.GradeStatus;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 年级状态变更事件
 */
public class GradeStatusChangedEvent extends BaseDomainEvent {

    private final Long gradeId;
    private final String gradeCode;
    private final String gradeName;
    private final GradeStatus oldStatus;
    private final GradeStatus newStatus;

    public GradeStatusChangedEvent(Grade grade, GradeStatus oldStatus, GradeStatus newStatus) {
        super("Grade", String.valueOf(grade.getId()));
        this.gradeId = grade.getId();
        this.gradeCode = grade.getGradeCode();
        this.gradeName = grade.getGradeName();
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public Long getGradeId() {
        return gradeId;
    }

    public String getGradeCode() {
        return gradeCode;
    }

    public String getGradeName() {
        return gradeName;
    }

    public GradeStatus getOldStatus() {
        return oldStatus;
    }

    public GradeStatus getNewStatus() {
        return newStatus;
    }
}
