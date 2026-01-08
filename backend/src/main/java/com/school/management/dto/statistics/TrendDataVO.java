package com.school.management.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 趋势数据VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataVO {

    /**
     * 趋势数据点列表
     */
    private List<TrendPointVO> trendPoints;

    /**
     * 汇总信息
     */
    private TrendSummaryVO summary;

    /**
     * 趋势数据点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPointVO {
        /**
         * 日期
         */
        private LocalDate date;

        /**
         * 日期标签（如：12/05 或 第12周）
         */
        private String dateLabel;

        /**
         * 检查次数
         */
        private Integer checkCount;

        /**
         * 总扣分
         */
        private BigDecimal totalScore;

        /**
         * 加权后扣分
         */
        private BigDecimal weightedScore;

        /**
         * 平均扣分
         */
        private BigDecimal avgScore;

        /**
         * 涉及班级数
         */
        private Integer classCount;

        /**
         * 涉及人次
         */
        private Integer personCount;

        /**
         * 扣分项触发次数
         */
        private Integer itemCount;
    }

    /**
     * 趋势汇总信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendSummaryVO {
        /**
         * 总检查次数
         */
        private Integer totalChecks;

        /**
         * 日均检查次数
         */
        private BigDecimal avgChecksPerDay;

        /**
         * 日均扣分
         */
        private BigDecimal avgScorePerDay;

        /**
         * 扣分趋势：up/down/stable
         */
        private String trend;

        /**
         * 趋势变化百分比
         */
        private BigDecimal trendPercentage;

        /**
         * 最高单日扣分
         */
        private BigDecimal maxDailyScore;

        /**
         * 最高单日扣分日期
         */
        private LocalDate maxScoreDate;

        /**
         * 最低单日扣分
         */
        private BigDecimal minDailyScore;

        /**
         * 最低单日扣分日期
         */
        private LocalDate minScoreDate;
    }

    /**
     * 班级趋势数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassTrendVO {
        private Long classId;
        private String className;
        private List<TrendPointVO> trendPoints;
    }
}
