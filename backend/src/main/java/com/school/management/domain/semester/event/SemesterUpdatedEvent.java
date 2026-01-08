package com.school.management.domain.semester.event;

import com.school.management.domain.shared.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * 学期更新事件
 */
public class SemesterUpdatedEvent implements DomainEvent {

    private final String semesterId;
    private final String semesterName;
    private final String semesterCode;
    private final LocalDateTime occurredOn;

    public SemesterUpdatedEvent(String semesterId, String semesterName, String semesterCode) {
        this.semesterId = semesterId;
        this.semesterName = semesterName;
        this.semesterCode = semesterCode;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public String aggregateId() {
        return semesterId;
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
