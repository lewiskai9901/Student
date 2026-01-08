package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生综测结果主表实体类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("student_evaluation_results")
public class StudentEvaluationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 综测周期ID
     */
    private Long evaluationPeriodId;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 姓名
     */
    private String studentName;

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
     * 系部ID
     */
    private Long departmentId;

    // ==================== 德育 ====================

    /**
     * 德育基础分
     */
    private BigDecimal moralBaseScore;

    /**
     * 德育奖励分
     */
    private BigDecimal moralBonusScore;

    /**
     * 德育扣分
     */
    private BigDecimal moralDeductScore;

    /**
     * 德育总分
     */
    private BigDecimal moralTotalScore;

    /**
     * 德育加权分
     */
    private BigDecimal moralWeightedScore;

    // ==================== 智育 ====================

    /**
     * 智育基础分
     */
    private BigDecimal intellectualBaseScore;

    /**
     * 智育奖励分
     */
    private BigDecimal intellectualBonusScore;

    /**
     * 智育扣分
     */
    private BigDecimal intellectualDeductScore;

    /**
     * 智育总分
     */
    private BigDecimal intellectualTotalScore;

    /**
     * 智育加权分
     */
    private BigDecimal intellectualWeightedScore;

    // ==================== 体育 ====================

    /**
     * 体育基础分
     */
    private BigDecimal physicalBaseScore;

    /**
     * 体育奖励分
     */
    private BigDecimal physicalBonusScore;

    /**
     * 体育扣分
     */
    private BigDecimal physicalDeductScore;

    /**
     * 体育总分
     */
    private BigDecimal physicalTotalScore;

    /**
     * 体育加权分
     */
    private BigDecimal physicalWeightedScore;

    // ==================== 美育 ====================

    /**
     * 美育基础分
     */
    private BigDecimal aestheticBaseScore;

    /**
     * 美育奖励分
     */
    private BigDecimal aestheticBonusScore;

    /**
     * 美育扣分
     */
    private BigDecimal aestheticDeductScore;

    /**
     * 美育总分
     */
    private BigDecimal aestheticTotalScore;

    /**
     * 美育加权分
     */
    private BigDecimal aestheticWeightedScore;

    // ==================== 劳育 ====================

    /**
     * 劳育基础分
     */
    private BigDecimal laborBaseScore;

    /**
     * 劳育奖励分
     */
    private BigDecimal laborBonusScore;

    /**
     * 劳育扣分
     */
    private BigDecimal laborDeductScore;

    /**
     * 劳育总分
     */
    private BigDecimal laborTotalScore;

    /**
     * 劳育加权分
     */
    private BigDecimal laborWeightedScore;

    // ==================== 发展素质 ====================

    /**
     * 发展素质基础分
     */
    private BigDecimal developmentBaseScore;

    /**
     * 发展素质奖励分
     */
    private BigDecimal developmentBonusScore;

    /**
     * 发展素质扣分
     */
    private BigDecimal developmentDeductScore;

    /**
     * 发展素质总分
     */
    private BigDecimal developmentTotalScore;

    /**
     * 发展素质加权分
     */
    private BigDecimal developmentWeightedScore;

    // ==================== 综合 ====================

    /**
     * 综测总分
     */
    private BigDecimal totalScore;

    /**
     * 班级排名
     */
    private Integer classRank;

    /**
     * 班级参评人数
     */
    private Integer classTotal;

    /**
     * 年级排名
     */
    private Integer gradeRank;

    /**
     * 年级参评人数
     */
    private Integer gradeTotal;

    /**
     * 系部排名
     */
    private Integer departmentRank;

    /**
     * 系部参评人数
     */
    private Integer departmentTotal;

    // ==================== 处分影响 ====================

    /**
     * 是否有处分
     */
    private Integer hasPunishment;

    /**
     * 处分类型
     */
    private Integer punishmentType;

    /**
     * 处分影响说明
     */
    private String punishmentEffect;

    // ==================== 状态 ====================

    /**
     * 状态: 0待计算,1已计算,2待审核,3已审核,4公示中,5已生效,6已归档
     */
    private Integer status;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 计算时间
     */
    private LocalDateTime calculatedAt;

    /**
     * 审核人
     */
    private Long reviewedBy;

    /**
     * 审核时间
     */
    private LocalDateTime reviewedAt;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ==================== 状态常量 ====================

    public static final int STATUS_PENDING = 0;        // 待计算
    public static final int STATUS_CALCULATED = 1;     // 已计算
    public static final int STATUS_TO_REVIEW = 2;      // 待审核
    public static final int STATUS_REVIEWED = 3;       // 已审核
    public static final int STATUS_PUBLICITY = 4;      // 公示中
    public static final int STATUS_EFFECTIVE = 5;      // 已生效
    public static final int STATUS_ARCHIVED = 6;       // 已归档

    /**
     * 计算总分
     */
    public void calculateTotalScore() {
        BigDecimal total = BigDecimal.ZERO;
        if (moralWeightedScore != null) total = total.add(moralWeightedScore);
        if (intellectualWeightedScore != null) total = total.add(intellectualWeightedScore);
        if (physicalWeightedScore != null) total = total.add(physicalWeightedScore);
        if (aestheticWeightedScore != null) total = total.add(aestheticWeightedScore);
        if (laborWeightedScore != null) total = total.add(laborWeightedScore);
        if (developmentWeightedScore != null) total = total.add(developmentWeightedScore);
        this.totalScore = total.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
