package com.school.management.domain.student.event;

import com.school.management.domain.student.model.Cohort;
import com.school.management.domain.student.model.CohortStatus;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 年级状态变更事件
 */
public class CohortStatusChangedEvent extends BaseDomainEvent {

    private final Long cohortId;
    private final String gradeCode;
    private final String gradeName;
    private final CohortStatus oldStatus;
    private final CohortStatus newStatus;

    public CohortStatusChangedEvent(Cohort cohort, CohortStatus oldStatus, CohortStatus newStatus) {
        super("Cohort", String.valueOf(cohort.getId()));
        this.cohortId = cohort.getId();
        this.gradeCode = cohort.getGradeCode();
        this.gradeName = cohort.getGradeName();
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public Long getCohortId() {
        return cohortId;
    }

    public String getGradeCode() {
        return gradeCode;
    }

    public String getGradeName() {
        return gradeName;
    }

    public CohortStatus getOldStatus() {
        return oldStatus;
    }

    public CohortStatus getNewStatus() {
        return newStatus;
    }
}
