package com.school.management.dto.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 年级统计VO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class GradeStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 班级数量
     */
    private Integer classCount;

    /**
     * 学生总数
     */
    private Integer studentCount;

    /**
     * 平均班级人数
     */
    private Double averageClassSize;

    /**
     * 标准班级人数
     */
    private Integer standardClassSize;

    /**
     * 最大班级人数
     */
    private Integer maxClassSize;

    /**
     * 最小班级人数
     */
    private Integer minClassSize;

    /**
     * 人数最多的班级名称
     */
    private String maxSizeClassName;

    /**
     * 人数最少的班级名称
     */
    private String minSizeClassName;

    /**
     * 本月申诉数量
     */
    private Integer monthlyAppealCount;

    /**
     * 本月申诉通过数量
     */
    private Integer monthlyApprovedCount;

    /**
     * 本月申诉通过率
     */
    private BigDecimal monthlyApprovalRate;

    /**
     * 本月申诉中分数调整总计
     */
    private BigDecimal monthlyScoreAdjustment;

    /**
     * 本学期申诉数量
     */
    private Integer semesterAppealCount;

    /**
     * 本学期申诉通过数量
     */
    private Integer semesterApprovedCount;

    /**
     * 本学期申诉通过率
     */
    private BigDecimal semesterApprovalRate;

    /**
     * 本学期分数调整总计
     */
    private BigDecimal semesterScoreAdjustment;

    /**
     * 本周检查次数
     */
    private Integer weeklyCheckCount;

    /**
     * 本月检查次数
     */
    private Integer monthlyCheckCount;

    /**
     * 本学期检查次数
     */
    private Integer semesterCheckCount;

    /**
     * 平均扣分(按班级)
     */
    private BigDecimal averageDeductionPerClass;

    /**
     * 平均扣分(按人均)
     */
    private BigDecimal averageDeductionPerStudent;

    /**
     * 班级人数方差(衡量班级规模差异)
     */
    private Double classSizeVariance;

    /**
     * 是否启用人数加权
     */
    private Boolean weightEnabled;

    /**
     * 获取申诉通过率百分比字符串
     */
    public String getMonthlyApprovalRatePercent() {
        if (monthlyApprovalRate == null) {
            return "0%";
        }
        return monthlyApprovalRate.multiply(BigDecimal.valueOf(100))
                .setScale(1, BigDecimal.ROUND_HALF_UP) + "%";
    }

    /**
     * 获取学期申诉通过率百分比字符串
     */
    public String getSemesterApprovalRatePercent() {
        if (semesterApprovalRate == null) {
            return "0%";
        }
        return semesterApprovalRate.multiply(BigDecimal.valueOf(100))
                .setScale(1, BigDecimal.ROUND_HALF_UP) + "%";
    }

    /**
     * 班级人数是否均衡
     */
    public Boolean isClassSizeBalanced() {
        if (classSizeVariance == null || standardClassSize == null) {
            return null;
        }
        // 方差小于标准人数的10%,认为比较均衡
        return classSizeVariance < (standardClassSize * 0.1);
    }
}
