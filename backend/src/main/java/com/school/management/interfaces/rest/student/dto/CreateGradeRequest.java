package com.school.management.interfaces.rest.student.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a grade.
 */
@Data
public class CreateGradeRequest {

    @NotBlank(message = "年级编码不能为空")
    private String gradeCode;

    @NotBlank(message = "年级名称不能为空")
    private String gradeName;

    @NotNull(message = "入学年份不能为空")
    private Integer enrollmentYear;

    private Integer schoolingYears = 3;

    private Integer standardClassSize = 50;
}
