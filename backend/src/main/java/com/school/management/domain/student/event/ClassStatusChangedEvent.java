package com.school.management.domain.student.event;

import com.school.management.domain.student.model.ClassStatus;
import com.school.management.domain.student.model.SchoolClass;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 班级状态变更事件
 */
public class ClassStatusChangedEvent extends BaseDomainEvent {

    private final Long classId;
    private final String classCode;
    private final String className;
    private final ClassStatus oldStatus;
    private final ClassStatus newStatus;
    private final Long updatedBy;

    public ClassStatusChangedEvent(SchoolClass schoolClass, ClassStatus oldStatus, ClassStatus newStatus) {
        super("SchoolClass", String.valueOf(schoolClass.getId()));
        this.classId = schoolClass.getId();
        this.classCode = schoolClass.getClassCode();
        this.className = schoolClass.getClassName();
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.updatedBy = schoolClass.getUpdatedBy();
    }

    public Long getClassId() {
        return classId;
    }

    public String getClassCode() {
        return classCode;
    }

    public String getClassName() {
        return className;
    }

    public ClassStatus getOldStatus() {
        return oldStatus;
    }

    public ClassStatus getNewStatus() {
        return newStatus;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    /**
     * 是否是毕业事件
     */
    public boolean isGraduation() {
        return newStatus == ClassStatus.GRADUATED;
    }

    /**
     * 是否是激活事件
     */
    public boolean isActivation() {
        return newStatus == ClassStatus.ACTIVE;
    }
}
