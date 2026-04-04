package com.school.management.application.calendar.command;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateAcademicYearCommand {
    private String yearCode;
    private String yearName;
    private LocalDate startDate;
    private LocalDate endDate;
}
