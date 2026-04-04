package com.school.management.domain.calendar.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class SemesterUpdatedEvent extends BaseDomainEvent {
    private final String semesterId;
    private final String semesterName;
    private final String semesterCode;

    public SemesterUpdatedEvent(String semesterId, String semesterName, String semesterCode) {
        super("Semester", semesterId);
        this.semesterId = semesterId;
        this.semesterName = semesterName;
        this.semesterCode = semesterCode;
    }

    public String getSemesterId() { return semesterId; }
    public String getSemesterName() { return semesterName; }
    public String getSemesterCode() { return semesterCode; }
}
