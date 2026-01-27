package com.school.management.interfaces.rest.teaching;

import lombok.Data;

import java.time.LocalDate;

/**
 * 更新学期请求
 */
@Data
public class UpdateSemesterRequest {

    /**
     * 学期名称
     */
    private String semesterName;

    /**
     * 学期开始日期
     */
    private LocalDate startDate;

    /**
     * 学期结束日期
     */
    private LocalDate endDate;

    /**
     * 教学开始日期
     */
    private LocalDate teachingStartDate;

    /**
     * 教学结束日期
     */
    private LocalDate teachingEndDate;

    /**
     * 考试周开始日期
     */
    private LocalDate examStartDate;

    /**
     * 考试周结束日期
     */
    private LocalDate examEndDate;

    /**
     * 教学周数
     */
    private Integer totalTeachingWeeks;
}
