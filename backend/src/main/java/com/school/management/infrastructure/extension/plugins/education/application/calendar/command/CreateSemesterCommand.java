package com.school.management.infrastructure.extension.plugins.education.application.calendar.command;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateSemesterCommand {
    private Long academicYearId;
    private String semesterName;
    private String semesterCode;
    private Integer semesterType;
    private LocalDate startDate;
    private LocalDate endDate;
}
