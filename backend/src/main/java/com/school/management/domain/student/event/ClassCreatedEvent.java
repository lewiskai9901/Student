package com.school.management.domain.student.event;

import com.school.management.domain.student.model.SchoolClass;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 班级创建事件
 */
public class ClassCreatedEvent extends BaseDomainEvent {

    private final Long orgUnitId;
    private final String classCode;
    private final String className;
    private final Long orgUnitId;
    private final Integer enrollmentYear;
    private final Long createdBy;

    public ClassCreatedEvent(SchoolClass schoolClass) {
        super("SchoolClass", String.valueOf(schoolClass.getId()));
        this.orgUnitId = schoolClass.getId();
        this.classCode = schoolClass.getClassCode();
        this.className = schoolClass.getClassName();
        this.orgUnitId = schoolClass.getOrgUnitId();
        this.enrollmentYear = schoolClass.getEnrollmentYear();
        this.createdBy = schoolClass.getCreatedBy();
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public String getClassCode() {
        return classCode;
    }

    public String getClassName() {
        return className;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public Integer getEnrollmentYear() {
        return enrollmentYear;
    }

    public Long getCreatedBy() {
        return createdBy;
    }
}
