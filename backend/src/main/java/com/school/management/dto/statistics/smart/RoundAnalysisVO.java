package com.school.management.dto.statistics.smart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 轮次分析VO
 * 分析多轮检查之间的变化情况
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundAnalysisVO {

    // ==================== 轮次配置分析 ====================

    /**
     * 最大轮次数
     */
    private Integer maxRounds;

    /**
     * 平均轮次数
     */
    private BigDecimal avgRounds;

    /**
     * 轮次分布统计
     */
    private List<RoundDistributionVO> roundDistribution;

    // ==================== 轮次对比分析 ====================

    /**
     * 各轮次对比数据
     */
    private List<RoundComparisonVO> roundComparison;

    /**
     * 整体改善率（末轮 vs 首轮）
     */
    private BigDecimal overallImprovementRate;

    /**
     * 改善班级数
     */
    private Integer improvedClasses;

    /**
     * 恶化班级数
     */
    private Integer worsenedClasses;

    /**
     * 持平班级数
     */
    private Integer stableClasses;

    // ==================== 洞察信息 ====================

    /**
     * 分析洞察
     */
    private List<String> insights;

    /**
     * 轮次分布
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundDistributionVO {
        /**
         * 轮次数
         */
        private Integer roundCount;

        /**
         * 检查记录数（有多少次检查是这个轮次数）
         */
        private Integer recordCount;

        /**
         * 涉及班级数
         */
        private Integer classCount;

        /**
         * 占比
         */
        private BigDecimal percentage;
    }

    /**
     * 轮次对比
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundComparisonVO {
        /**
         * 轮次
         */
        private Integer round;

        /**
         * 参与班级数
         */
        private Integer classCount;

        /**
         * 该轮平均扣分
         */
        private BigDecimal avgScore;

        /**
         * 该轮总扣分
         */
        private BigDecimal totalScore;

        /**
         * 与上一轮对比变化（负数表示改善）
         */
        private BigDecimal vsPreRound;

        /**
         * 改善的班级数
         */
        private Integer improvedCount;

        /**
         * 恶化的班级数
         */
        private Integer worsenedCount;

        /**
         * 持平的班级数
         */
        private Integer stableCount;

        /**
         * 改善率
         */
        private BigDecimal improvementRate;
    }
}
