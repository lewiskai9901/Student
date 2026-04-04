package com.school.management.application.calendar.command;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateAcademicEventCommand {
    private Long yearId;
    private Long semesterId;
    private String eventName;
    private Integer eventType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean allDay;
    private String description;
}
