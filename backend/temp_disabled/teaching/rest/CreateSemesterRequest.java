package com.school.management.interfaces.rest.teaching;

import lombok.Data;

import java.time.LocalDate;

/**
 * 创建学期请求
 */
@Data
public class CreateSemesterRequest {

    /**
     * 所属学年ID
     */
    private Long academicYearId;

    /**
     * 学期代码
     */
    private String semesterCode;

    /**
     * 学期名称
     */
    private String semesterName;

    /**
     * 学期类型：1秋季 2春季
     */
    private Integer semesterType;

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
    private Integer totalTeachingWeeks = 18;
}
