package com.school.management.application.calendar.command;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateAcademicEventCommand {
    private String eventName;
    private Integer eventType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private Integer affectType;
    private Integer substituteWeekday;
    private String affectSlots;
}
