package com.school.management.interfaces.rest.semester;

import lombok.Data;

import java.time.LocalDate;

/**
 * 更新学期请求 DTO
 */
@Data
public class UpdateSemesterRequest {

    private String semesterName;

    private LocalDate startDate;

    private LocalDate endDate;
}
