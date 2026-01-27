package com.school.management.interfaces.rest.teaching;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建课程请求
 */
@Data
public class CreateCourseRequest {

    @NotBlank(message = "课程代码不能为空")
    private String courseCode;

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    private String englishName;

    @NotNull(message = "课程类型不能为空")
    private Integer courseType;

    @NotNull(message = "课程性质不能为空")
    private Integer courseNature;

    @NotNull(message = "学分不能为空")
    @Positive(message = "学分必须大于0")
    private BigDecimal credits;

    @NotNull(message = "总学时不能为空")
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
}
