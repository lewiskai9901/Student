package com.school.management.interfaces.rest.semester;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建学期请求 DTO
 */
@Data
public class CreateSemesterRequest {

    @NotBlank(message = "学期名称不能为空")
    private String semesterName;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @NotNull(message = "开始年份不能为空")
    private Integer startYear;

    @NotNull(message = "学期类型不能为空")
    private Integer semesterType;
}
