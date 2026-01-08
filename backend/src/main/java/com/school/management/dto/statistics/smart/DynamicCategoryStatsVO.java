package com.school.management.dto.statistics.smart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 动态类别统计VO
 * 自动识别并统计检查计划涉及的所有类别
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicCategoryStatsVO {

    /**
     * 自动识别到的类别列表
     */
    private List<DetectedCategoryVO> detectedCategories;

    /**
     * 各类别详细统计
     */
    private List<CategoryStatsDetailVO> categoryStats;

    /**
     * 总扣分
     */
    private BigDecimal totalScore;

    /**
     * 问题最多的类别
     */
    private String topCategory;

    /**
     * 问题最多类别的占比
     */
    private BigDecimal topCategoryPercentage;

    /**
     * 识别到的类别
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectedCategoryVO {
        private Long categoryId;
        private String categoryCode;
        private String categoryName;
        private String categoryType; // HYGIENE, DISCIPLINE, OTHER
        private Integer checkCount; // 多少次检查涉及该类别
    }

    /**
     * 类别详细统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStatsDetailVO {
        /**
         * 类别ID
         */
        private Long categoryId;

        /**
         * 类别编码
         */
        private String categoryCode;

        /**
         * 类别名称
         */
        private String categoryName;

        /**
         * 类别类型
         */
        private String categoryType;

        /**
         * 总扣分
         */
        private BigDecimal totalScore;

        /**
         * 占总扣分的百分比
         */
        private BigDecimal percentage;

        /**
         * 扣分项触发次数
         */
        private Integer triggerCount;

        /**
         * 涉及班级数
         */
        private Integer affectedClasses;

        /**
         * 涉及人次
         */
        private Integer personCount;

        /**
         * 该类别下的高频扣分项
         */
        private List<TopItemVO> topItems;

        /**
         * 轮次分布（如果有多轮检查）
         */
        private List<RoundDistVO> roundDistribution;

        /**
         * 趋势数据点
         */
        private List<TrendPointVO> trendData;
    }

    /**
     * 高频扣分项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopItemVO {
        private Long itemId;
        private String itemName;
        private Integer triggerCount;
        private BigDecimal totalScore;
        private Integer classCount; // 涉及班级数
    }

    /**
     * 轮次分布
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundDistVO {
        private Integer round;
        private BigDecimal score;
        private Integer count;
    }

    /**
     * 趋势数据点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPointVO {
        private String date;
        private String dateLabel;
        private BigDecimal score;
        private Integer count;
    }
}
