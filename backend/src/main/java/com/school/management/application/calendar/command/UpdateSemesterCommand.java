package com.school.management.application.calendar.command;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateSemesterCommand {
    private String semesterName;
    private Integer semesterType;
    private LocalDate startDate;
    private LocalDate endDate;
}
