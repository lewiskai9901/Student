package com.school.management.application.teaching.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 更新学期命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSemesterCommand {

    private Long id;

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

    /**
     * 操作人ID
     */
    private Long operatorId;
}
