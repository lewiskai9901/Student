package com.school.management.dto.statistics.smart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 智能统计概览VO
 * 包含覆盖率、趋势指示、警告信息等智能分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartStatisticsOverviewVO {

    // ==================== 基本信息 ====================

    /**
     * 计划ID
     */
    private Long planId;

    /**
     * 计划名称
     */
    private String planName;

    /**
     * 计划状态
     */
    private Integer status;

    /**
     * 计划开始日期
     */
    private LocalDate startDate;

    /**
     * 计划结束日期
     */
    private LocalDate endDate;

    // ==================== 检查统计 ====================

    /**
     * 总检查次数（检查记录数）
     */
    private Integer totalChecks;

    /**
     * 涉及班级数
     */
    private Integer totalClasses;

    /**
     * 总检查轮次（所有检查的轮次总和）
     */
    private Integer totalRounds;

    /**
     * 检查覆盖率信息
     */
    private CheckCoverageVO coverage;

    // ==================== 扣分统计 ====================

    /**
     * 总扣分
     */
    private BigDecimal totalScore;

    /**
     * 平均每次检查扣分
     */
    private BigDecimal avgScorePerCheck;

    /**
     * 平均每轮扣分（针对轮次不一致的情况）
     */
    private BigDecimal avgScorePerRound;

    /**
     * 最高单班扣分
     */
    private BigDecimal maxScore;

    /**
     * 最低单班扣分
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

    // ==================== 趋势信息 ====================

    /**
     * 与上期对比的趋势
     */
    private TrendInfo trend;

    /**
     * 最近检查日期
     */
    private LocalDate lastCheckDate;

    // ==================== 加权信息 ====================

    /**
     * 是否启用加权
     */
    private Boolean weightEnabled;

    /**
     * 加权方案名称
     */
    private String weightConfigName;

    /**
     * 加权后总扣分
     */
    private BigDecimal weightedTotalScore;

    // ==================== 智能提示 ====================

    /**
     * 警告信息列表（如缺检提醒、数据异常等）
     */
    private List<String> warnings;

    /**
     * 洞察信息列表（自动生成的分析结论）
     */
    private List<String> insights;

    /**
     * 趋势信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendInfo {
        /**
         * 趋势方向：up=上升（变差）, down=下降（变好）, stable=稳定
         */
        private String direction;

        /**
         * 变化百分比（正数表示变差，负数表示变好）
         */
        private BigDecimal percentage;

        /**
         * 变化绝对值
         */
        private BigDecimal changeValue;

        /**
         * 对比基准描述（如"较上周"、"较上月"）
         */
        private String compareBase;
    }
}
