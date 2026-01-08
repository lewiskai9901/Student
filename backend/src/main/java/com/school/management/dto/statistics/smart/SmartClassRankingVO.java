package com.school.management.dto.statistics.smart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 智能班级排名VO
 * 支持多维度排名、轮次信息、趋势分析
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartClassRankingVO {

    // ==================== 基本信息 ====================

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 班主任姓名
     */
    private String teacherName;

    /**
     * 班级人数
     */
    private Integer studentCount;

    // ==================== 检查参与度 ====================

    /**
     * 被检查次数
     */
    private Integer checkCount;

    /**
     * 总检查轮次（某些检查可能有多轮）
     */
    private Integer totalRounds;

    /**
     * 参与率（实际检查次数 / 计划检查次数）
     */
    private BigDecimal participationRate;

    // ==================== 多维度得分 ====================

    /**
     * 原始总扣分
     */
    private BigDecimal totalScore;

    /**
     * 加权后总扣分
     */
    private BigDecimal weightedScore;

    /**
     * 每次检查平均扣分
     */
    private BigDecimal avgScorePerCheck;

    /**
     * 每轮平均扣分（推荐用于排名）
     */
    private BigDecimal avgScorePerRound;

    /**
     * 归一化得分（0-100，用于不同检查计划间对比）
     */
    private BigDecimal normalizedScore;

    // ==================== 排名信息 ====================

    /**
     * 全校排名（基于当前排序模式）
     */
    private Integer ranking;

    /**
     * 年级内排名
     */
    private Integer gradeRanking;

    /**
     * 部门内排名
     */
    private Integer departmentRanking;

    /**
     * 等级：优秀/良好/中等/较差
     */
    private String scoreLevel;

    /**
     * 与全校平均值差距
     */
    private BigDecimal vsAvg;

    /**
     * 与年级平均值差距
     */
    private BigDecimal vsGradeAvg;

    // ==================== 趋势分析 ====================

    /**
     * 趋势方向：up=变差, down=变好, stable=稳定
     */
    private String trend;

    /**
     * 趋势变化值（与上期对比）
     */
    private BigDecimal trendValue;

    /**
     * 轮次间改善率（多轮检查时，后面轮次相比前面的改善程度）
     */
    private BigDecimal roundImprovementRate;

    // ==================== 类别得分明细 ====================

    /**
     * 各类别扣分明细
     */
    private List<CategoryScoreDetailVO> categoryScores;

    /**
     * 轮次得分明细（如果有多轮）
     */
    private List<RoundScoreVO> roundScores;

    /**
     * 类别扣分明细
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryScoreDetailVO {
        private Long categoryId;
        private String categoryName;
        private String categoryCode;
        private BigDecimal score;
        private BigDecimal percentage; // 占总扣分的百分比
        private Integer itemCount; // 扣分项数量
    }

    /**
     * 轮次得分
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundScoreVO {
        private Integer round;
        private BigDecimal score;
        private BigDecimal vsPreRound; // 与上一轮对比
        private String improvement; // improved/worsened/stable
    }
}
