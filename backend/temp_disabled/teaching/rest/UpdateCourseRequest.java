package com.school.management.interfaces.rest.teaching;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新课程请求
 */
@Data
public class UpdateCourseRequest {

    private String courseName;

    private String englishName;

    private Integer courseType;

    private Integer courseNature;

    @Positive(message = "学分必须大于0")
    private BigDecimal credits;

    @Positive(message = "总学时必须大于0")
    private Integer totalHours;

    private Integer theoryHours;

    private Integer labHours;

    private Integer practiceHours;

    private Integer weeklyHours;

    private Long departmentId;

    private Integer examType;

    private Integer gradeType;

    private String prerequisites;

    private String description;

    private String syllabus;

    private Integer status;
}
