package com.school.management.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 检查计划统计概览VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanStatisticsOverviewVO {

    /**
     * 计划ID
     */
    private Long planId;

    /**
     * 计划名称
     */
    private String planName;

    /**
     * 总检查次数
     */
    private Integer totalChecks;

    /**
     * 涉及班级数
     */
    private Integer totalClasses;

    /**
     * 总扣分
     */
    private BigDecimal totalScore;

    /**
     * 平均扣分（每次检查）
     */
    private BigDecimal avgScore;

    /**
     * 最高单次扣分
     */
    private BigDecimal maxScore;

    /**
     * 最低单次扣分
     */
    private BigDecimal minScore;

    /**
     * 扣分项触发总次数
     */
    private Integer totalItems;

    /**
     * 涉及总人次
     */
    private Integer totalPersons;

    /**
     * 是否启用加权
     */
    private Boolean weightEnabled;

    /**
     * 加权方案名称
     */
    private String weightConfigName;

    /**
     * 最近检查日期
     */
    private LocalDate lastCheckDate;

    /**
     * 计划开始日期
     */
    private LocalDate startDate;

    /**
     * 计划结束日期
     */
    private LocalDate endDate;

    /**
     * 计划状态
     */
    private Integer status;
}
