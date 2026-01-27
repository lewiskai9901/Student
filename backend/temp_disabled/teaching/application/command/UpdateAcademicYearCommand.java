package com.school.management.application.teaching.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 更新学年命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAcademicYearCommand {

    private Long id;

    /**
     * 学年代码
     */
    private String yearCode;

    /**
     * 学年名称
     */
    private String yearName;

    /**
     * 学年开始日期
     */
    private LocalDate startDate;

    /**
     * 学年结束日期
     */
    private LocalDate endDate;

    /**
     * 操作人ID
     */
    private Long operatorId;
}
