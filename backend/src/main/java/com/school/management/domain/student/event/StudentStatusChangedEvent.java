package com.school.management.domain.student.event;

import com.school.management.domain.shared.event.DomainEvent;
import com.school.management.domain.student.model.valueobject.StudentStatus;

/**
 * 学籍状态变更事件
 */
public class StudentStatusChangedEvent extends DomainEvent {

    private final String studentNo;
    private final StudentStatus oldStatus;
    private final StudentStatus newStatus;
    private final String reason;

    public StudentStatusChangedEvent(
            String aggregateId,
            String studentNo,
            StudentStatus oldStatus,
            StudentStatus newStatus,
            String reason
    ) {
        super("Student", aggregateId);
        this.studentNo = studentNo;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public StudentStatus getOldStatus() {
        return oldStatus;
    }

    public StudentStatus getNewStatus() {
        return newStatus;
    }

    public String getReason() {
        return reason;
    }
}
