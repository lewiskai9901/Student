package com.school.management.application.teaching.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 教学周DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeachingWeekDTO {

    private Long id;

    /**
     * 所属学期ID
     */
    private Long semesterId;

    /**
     * 周次
     */
    private Integer weekNumber;

    /**
     * 本周开始日期
     */
    private LocalDate startDate;

    /**
     * 本周结束日期
     */
    private LocalDate endDate;

    /**
     * 周类型：1正常教学周 2考试周 3假期周 4实践周
     */
    private Integer weekType;

    /**
     * 周类型名称
     */
    private String weekTypeName;

    /**
     * 周标签
     */
    private String weekLabel;

    /**
     * 是否有效
     */
    private Boolean isActive;

    /**
     * 备注
     */
    private String remark;
}
