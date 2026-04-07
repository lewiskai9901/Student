package com.school.management.domain.student.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 学生入学事件
 */
public class StudentEnrolledEvent extends BaseDomainEvent {

    private final String studentNo;
    private final String studentName;
    private final Long orgUnitId;

    public StudentEnrolledEvent(String aggregateId, String studentNo, String studentName, Long orgUnitId) {
        super("Student", aggregateId);
        this.studentNo = studentNo;
        this.studentName = studentName;
        this.orgUnitId = orgUnitId;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }
}
