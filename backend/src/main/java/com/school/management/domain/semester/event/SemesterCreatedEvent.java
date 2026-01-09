package com.school.management.domain.semester.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 学期创建事件
 */
public class SemesterCreatedEvent extends BaseDomainEvent {

    private final String semesterId;
    private final String semesterName;
    private final String semesterCode;

    public SemesterCreatedEvent(String semesterId, String semesterName, String semesterCode) {
        super("Semester", semesterId);
        this.semesterId = semesterId;
        this.semesterName = semesterName;
        this.semesterCode = semesterCode;
    }

    public String getSemesterId() {
        return semesterId;
    }

    public String getSemesterName() {
        return semesterName;
    }

    public String getSemesterCode() {
        return semesterCode;
    }
}
