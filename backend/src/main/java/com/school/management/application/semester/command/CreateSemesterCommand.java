package com.school.management.application.semester.command;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建学期命令
 */
@Data
@Builder
public class CreateSemesterCommand {

    /**
     * 学期名称
     */
    private String semesterName;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 开始年份
     */
    private Integer startYear;

    /**
     * 学期类型: 1第一学期 2第二学期
     */
    private Integer semesterType;

    /**
     * 创建人ID
     */
    private Long createdBy;
}
