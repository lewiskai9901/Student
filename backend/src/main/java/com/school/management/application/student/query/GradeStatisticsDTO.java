package com.school.management.application.student.query;

import lombok.Data;

/**
 * Data Transfer Object for Grade Statistics.
 */
@Data
public class GradeStatisticsDTO {
    private Long gradeId;
    private String gradeName;
    private Integer enrollmentYear;
    private Integer classCount;
    private Integer studentCount;
    private Double avgStudentsPerClass;
    private Integer maleCount;
    private Integer femaleCount;
}
