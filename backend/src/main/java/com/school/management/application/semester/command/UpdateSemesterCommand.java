package com.school.management.application.semester.command;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 更新学期命令
 */
@Data
@Builder
public class UpdateSemesterCommand {

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
     * 更新人ID
     */
    private Long updatedBy;
}
