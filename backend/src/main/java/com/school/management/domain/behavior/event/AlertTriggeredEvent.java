package com.school.management.domain.behavior.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class AlertTriggeredEvent extends BaseDomainEvent {

    private final Long alertId;
    private final Long studentId;
    private final Long classId;
    private final String alertType;
    private final String alertLevel;
    private final String title;

    public AlertTriggeredEvent(Long alertId, Long studentId, Long classId,
                                String alertType, String alertLevel, String title) {
        super("BehaviorAlert", alertId);
        this.alertId = alertId;
        this.studentId = studentId;
        this.classId = classId;
        this.alertType = alertType;
        this.alertLevel = alertLevel;
        this.title = title;
    }

    public Long getAlertId() { return alertId; }
    public Long getStudentId() { return studentId; }
    public Long getClassId() { return classId; }
    public String getAlertType() { return alertType; }
    public String getAlertLevel() { return alertLevel; }
    public String getTitle() { return title; }
}
