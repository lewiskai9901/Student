package com.school.management.domain.asset.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 学生退宿事件
 */
public class StudentCheckedOutEvent extends BaseDomainEvent {

    private final Long studentId;
    private final String studentName;
    private final String dormitoryNo;
    private final Integer bedNumber;

    public StudentCheckedOutEvent(String aggregateId, Long studentId, String studentName,
                                   String dormitoryNo, Integer bedNumber) {
        super("Dormitory", aggregateId);
        this.studentId = studentId;
        this.studentName = studentName;
        this.dormitoryNo = dormitoryNo;
        this.bedNumber = bedNumber;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getDormitoryNo() {
        return dormitoryNo;
    }

    public Integer getBedNumber() {
        return bedNumber;
    }
}
