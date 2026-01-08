package com.school.management.domain.student.event;

import com.school.management.domain.shared.event.DomainEvent;

/**
 * 学生信息更新事件
 */
public class StudentUpdatedEvent extends DomainEvent {

    private final String studentNo;
    private final String studentName;

    public StudentUpdatedEvent(String aggregateId, String studentNo, String studentName) {
        super("Student", aggregateId);
        this.studentNo = studentNo;
        this.studentName = studentName;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public String getStudentName() {
        return studentName;
    }
}
