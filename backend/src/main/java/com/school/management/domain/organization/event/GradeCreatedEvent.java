package com.school.management.domain.organization.event;

import com.school.management.domain.organization.model.Grade;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 年级创建事件
 */
public class GradeCreatedEvent extends BaseDomainEvent {

    private final Long gradeId;
    private final String gradeCode;
    private final String gradeName;
    private final Integer enrollmentYear;
    private final Integer schoolingYears;
    private final Long createdBy;

    public GradeCreatedEvent(Grade grade) {
        super("Grade", String.valueOf(grade.getId()));
        this.gradeId = grade.getId();
        this.gradeCode = grade.getGradeCode();
        this.gradeName = grade.getGradeName();
        this.enrollmentYear = grade.getEnrollmentYear();
        this.schoolingYears = grade.getSchoolingYears();
        this.createdBy = grade.getCreatedBy();
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
