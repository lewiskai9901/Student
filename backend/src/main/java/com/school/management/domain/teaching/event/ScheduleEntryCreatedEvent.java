package com.school.management.domain.teaching.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class ScheduleEntryCreatedEvent extends BaseDomainEvent {
    private final Long entryId;
    private final Long taskId;
    private final Long semesterId;

    public ScheduleEntryCreatedEvent(Long entryId, Long taskId, Long semesterId) {
        super("ScheduleEntry", entryId != null ? entryId.toString() : null);
        this.entryId = entryId;
        this.taskId = taskId;
        this.semesterId = semesterId;
    }

    public Long getEntryId() { return entryId; }
    public Long getTaskId() { return taskId; }
    public Long getSemesterId() { return semesterId; }
}
