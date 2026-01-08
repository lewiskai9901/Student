package com.school.management.dto.statistics.smart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 班级追踪VO
 * 追踪单个班级在检查计划下的历史表现
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassTrackingVO {

    // ==================== 班级信息 ====================

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 班主任
     */
    private String teacherName;

    /**
     * 班级人数
     */
    private Integer studentCount;

    // ==================== 整体统计 ====================

    /**
     * 被检查次数
     */
    private Integer totalChecks;

    /**
     * 总轮次
     */
    private Integer totalRounds;

    /**
     * 总扣分
     */
    private BigDecimal totalScore;

    /**
     * 平均每次扣分
     */
    private BigDecimal avgScorePerCheck;

    /**
     * 平均每轮扣分
     */
    private BigDecimal avgScorePerRound;

    /**
     * 全校排名
     */
    private Integer ranking;

    /**
     * 年级排名
     */
    private Integer gradeRanking;

    /**
     * 等级
     */
    private String scoreLevel;

    // ==================== 与平均对比 ====================

    /**
     * 与全校平均对比
     */
    private BigDecimal vsOverallAvg;

    /**
     * 与年级平均对比
     */
    private BigDecimal vsGradeAvg;

    /**
     * 表现评价：excellent/good/average/poor
     */
    private String performance;

    // ==================== 历史记录 ====================

    /**
     * 检查历史
     */
    private List<CheckHistoryItemVO> checkHistory;

    /**
     * 类别趋势
     */
    private List<CategoryTrendVO> categoryTrend;

    /**
     * 月度统计
     */
    private List<MonthlyStatsVO> monthlyStats;

    // ==================== 高频问题 ====================

    /**
     * 该班级的高频扣分项
     */
    private List<FrequentItemVO> frequentItems;

    /**
     * 检查历史项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckHistoryItemVO {
        private Long recordId;
        private String recordName;
        private LocalDate checkDate;
        private Integer rounds;
        private BigDecimal totalScore;
        private BigDecimal avgScorePerRound;
        private Integer ranking;
        private String trend; // vs previous: up/down/stable
    }

    /**
     * 类别趋势
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryTrendVO {
        private Long categoryId;
        private String categoryName;
        private BigDecimal totalScore;
        private BigDecimal percentage;
        private String trend; // 趋势：improving/worsening/stable
        private List<TrendPointVO> trendPoints;
    }

    /**
     * 趋势点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPointVO {
        private String date;
        private BigDecimal score;
    }

    /**
     * 月度统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStatsVO {
        private String month;
        private Integer checkCount;
        private BigDecimal totalScore;
        private BigDecimal avgScore;
        private Integer ranking;
    }

    /**
     * 高频扣分项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrequentItemVO {
        private Long itemId;
        private String itemName;
        private String categoryName;
        private Integer triggerCount;
        private BigDecimal totalScore;
    }
}
