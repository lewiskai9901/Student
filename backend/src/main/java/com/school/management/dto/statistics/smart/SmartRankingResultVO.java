package com.school.management.dto.statistics.smart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 智能排名结果VO
 * 包含排名列表和相关元信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartRankingResultVO {

    // ==================== 排名列表 ====================

    /**
     * 班级排名列表
     */
    private List<SmartClassRankingVO> rankings;

    /**
     * 总数
     */
    private Long total;

    /**
     * 当前页
     */
    private Integer pageNum;

    /**
     * 每页数量
     */
    private Integer pageSize;

    // ==================== 覆盖信息 ====================

    /**
     * 检查覆盖率信息
     */
    private CheckCoverageVO coverage;

    /**
     * 可比较集合信息
     */
    private ComparableSetVO comparableSet;

    // ==================== 统计摘要 ====================

    /**
     * 全校平均分
     */
    private BigDecimal overallAvgScore;

    /**
     * 各年级平均分
     */
    private List<GradeAvgVO> gradeAvgScores;

    /**
     * 当前使用的对比模式
     */
    private String compareMode;

    /**
     * 排序字段
     */
    private String sortBy;

    /**
     * 排序方向
     */
    private String sortOrder;

    // ==================== 警告和提示 ====================

    /**
     * 警告信息
     */
    private List<String> warnings;

    /**
     * 提示信息
     */
    private List<String> tips;

    /**
     * 可比较集合信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparableSetVO {
        /**
         * 共同被检查的班级数
         */
        private Integer commonClassCount;

        /**
         * 共同的检查类别数
         */
        private Integer commonCategoryCount;

        /**
         * 共同的最小轮次数
         */
        private Integer commonMinRounds;

        /**
         * 是否完全可比
         */
        private Boolean fullyComparable;

        /**
         * 不可比原因（如果不完全可比）
         */
        private List<String> nonComparableReasons;
    }

    /**
     * 年级平均分
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeAvgVO {
        private Long gradeId;
        private String gradeName;
        private BigDecimal avgScore;
        private Integer classCount;
    }
}
