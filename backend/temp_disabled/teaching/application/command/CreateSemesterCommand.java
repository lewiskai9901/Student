package com.school.management.application.teaching.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 创建学期命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSemesterCommand {

    /**
     * 所属学年ID
     */
    private Long academicYearId;

    /**
     * 学期代码，如2025-2026-1
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
    @Builder.Default
    private Integer totalTeachingWeeks = 18;

    /**
     * 操作人ID
     */
    private Long operatorId;
}
