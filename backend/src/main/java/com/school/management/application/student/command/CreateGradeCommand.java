package com.school.management.application.student.command;

import lombok.Data;

/**
 * Command for creating a new grade.
 */
@Data
public class CreateGradeCommand {
    private String gradeCode;
    private String gradeName;
    private Integer enrollmentYear;
    private Integer schoolingYears;
    private Integer standardClassSize;
    private Long createdBy;
}
