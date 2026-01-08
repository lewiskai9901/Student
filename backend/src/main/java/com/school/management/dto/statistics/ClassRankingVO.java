package com.school.management.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 班级排名VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassRankingVO {

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

    /**
     * 检查次数
     */
    private Integer checkCount;

    /**
     * 总扣分（原始分）
     */
    private BigDecimal totalScore;

    /**
     * 加权后扣分
     */
    private BigDecimal weightedScore;

    /**
     * 平均每次检查扣分
     */
    private BigDecimal avgScorePerCheck;

    /**
     * 平均分（用于导出）
     */
    private BigDecimal avgScore;

    /**
     * 全校排名（扣分少的排名靠前）
     */
    private Integer ranking;

    /**
     * 年级内排名
     */
    private Integer gradeRanking;

    /**
     * 等级：优秀/良好/中等/较差
     */
    private String scoreLevel;

    /**
     * 与平均值差距（正数表示比平均差，负数表示比平均好）
     */
    private BigDecimal vsAvg;

    /**
     * 各类别扣分明细
     */
    private List<CategoryScoreVO> categoryScores;

    /**
     * 类别扣分明细
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryScoreVO {
        private Long categoryId;
        private String categoryName;
        private BigDecimal score;
        private BigDecimal percentage;
    }
}
