package com.school.management.domain.calendar.model.entity;

import com.school.management.domain.calendar.model.valueobject.EventType;
import java.time.LocalDate;

public class AcademicEvent {
    private Long id;
    private Long yearId;
    private Long semesterId;
    private String eventName;
    private EventType eventType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean allDay;
    private String description;

    protected AcademicEvent() {}

    public static AcademicEvent create(Long yearId, Long semesterId, String eventName,
                                        EventType eventType, LocalDate startDate, LocalDate endDate,
                                        Boolean allDay, String description) {
        if (eventName == null || eventName.trim().isEmpty()) throw new IllegalArgumentException("事件名称不能为空");
        if (startDate == null) throw new IllegalArgumentException("开始日期不能为空");
        AcademicEvent event = new AcademicEvent();
        event.yearId = yearId;
        event.semesterId = semesterId;
        event.eventName = eventName;
        event.eventType = eventType != null ? eventType : EventType.OTHER;
        event.startDate = startDate;
        event.endDate = endDate;
        event.allDay = allDay != null ? allDay : true;
        event.description = description;
        return event;
    }

    public static AcademicEvent reconstruct(Long id, Long yearId, Long semesterId, String eventName,
                                             EventType eventType, LocalDate startDate, LocalDate endDate,
                                             Boolean allDay, String description) {
        AcademicEvent event = new AcademicEvent();
        event.id = id;
        event.yearId = yearId;
        event.semesterId = semesterId;
        event.eventName = eventName;
        event.eventType = eventType;
        event.startDate = startDate;
        event.endDate = endDate;
        event.allDay = allDay;
        event.description = description;
        return event;
    }

    public void update(String eventName, EventType eventType, LocalDate startDate,
                       LocalDate endDate, String description) {
        if (eventName == null || eventName.trim().isEmpty()) throw new IllegalArgumentException("事件名称不能为空");
        this.eventName = eventName;
        this.eventType = eventType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getYearId() { return yearId; }
    public Long getSemesterId() { return semesterId; }
    public String getEventName() { return eventName; }
    public EventType getEventType() { return eventType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Boolean getAllDay() { return allDay; }
    public String getDescription() { return description; }
}
