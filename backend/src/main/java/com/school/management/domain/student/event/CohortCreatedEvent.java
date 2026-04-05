package com.school.management.domain.student.event;

import com.school.management.domain.student.model.Cohort;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 年级创建事件
 */
public class CohortCreatedEvent extends BaseDomainEvent {

    private final Long cohortId;
    private final String gradeCode;
    private final String gradeName;
    private final Integer enrollmentYear;
    private final Integer schoolingYears;
    private final Long createdBy;

    public CohortCreatedEvent(Cohort cohort) {
        super("Cohort", String.valueOf(cohort.getId()));
        this.cohortId = cohort.getId();
        this.gradeCode = cohort.getGradeCode();
        this.gradeName = cohort.getGradeName();
        this.enrollmentYear = cohort.getEnrollmentYear();
        this.schoolingYears = cohort.getSchoolingYears();
        this.createdBy = cohort.getCreatedBy();
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

    public Integer getEnrollmentYear() {
        return enrollmentYear;
    }

    public Integer getSchoolingYears() {
        return schoolingYears;
    }

    public Long getCreatedBy() {
        return createdBy;
    }
}
