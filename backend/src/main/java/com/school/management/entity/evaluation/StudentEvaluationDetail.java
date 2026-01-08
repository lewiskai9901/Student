package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生综测明细实体类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("student_evaluation_details")
public class StudentEvaluationDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 综测结果ID
     */
    private Long resultId;

    /**
     * 综测周期ID
     */
    private Long evaluationPeriodId;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 明细类型: QUANTIFICATION/RATING/HONOR/PUNISHMENT/SCORE/MANUAL
     */
    private String detailType;

    /**
     * 影响维度
     */
    private String evaluationDimension;

    /**
     * 分数类别: BASE/BONUS/DEDUCT
     */
    private String scoreCategory;

    /**
     * 数据来源: CHECK_ITEM/RATING_RESULT/HONOR_APPLICATION/PUNISHMENT/ACADEMIC_SCORE/MANUAL_INPUT
     */
    private String sourceType;

    /**
     * 来源记录ID
     */
    private Long sourceId;

    /**
     * 来源编码
     */
    private String sourceCode;

    /**
     * 来源名称/描述
     */
    private String sourceName;

    /**
     * 发生日期
     */
    private LocalDate sourceDate;

    /**
     * 分数(正数加分,负数扣分)
     */
    private BigDecimal score;

    /**
     * 封顶前分数
     */
    private BigDecimal scoreBeforeCap;

    /**
     * 是否被封顶
     */
    private Integer isCapped;

    /**
     * 封顶原因
     */
    private String capReason;

    /**
     * 行为类型ID
     */
    private Long behaviorTypeId;

    /**
     * 行为编码
     */
    private String behaviorTypeCode;

    /**
     * 是否确认有效
     */
    private Integer confirmed;

    /**
     * 是否被排除
     */
    private Integer excluded;

    /**
     * 排除原因
     */
    private String excludeReason;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ==================== 明细类型常量 ====================

    public static final String TYPE_QUANTIFICATION = "QUANTIFICATION";  // 量化扣分
    public static final String TYPE_RATING = "RATING";                  // 评级结果
    public static final String TYPE_HONOR = "HONOR";                    // 荣誉加分
    public static final String TYPE_PUNISHMENT = "PUNISHMENT";          // 处分影响
    public static final String TYPE_SCORE = "SCORE";                    // 学业成绩
    public static final String TYPE_MANUAL = "MANUAL";                  // 手动调整

    // ==================== 分数类别常量 ====================

    public static final String CATEGORY_BASE = "BASE";                  // 基础分
    public static final String CATEGORY_BONUS = "BONUS";                // 奖励分
    public static final String CATEGORY_DEDUCT = "DEDUCT";              // 扣分

    // ==================== 数据来源常量 ====================

    public static final String SOURCE_CHECK_ITEM = "CHECK_ITEM";               // 扣分项
    public static final String SOURCE_RATING_RESULT = "RATING_RESULT";         // 评级结果
    public static final String SOURCE_HONOR_APPLICATION = "HONOR_APPLICATION"; // 荣誉申报
    public static final String SOURCE_PUNISHMENT = "PUNISHMENT";               // 处分记录
    public static final String SOURCE_ACADEMIC_SCORE = "ACADEMIC_SCORE";       // 学业成绩
    public static final String SOURCE_MANUAL_INPUT = "MANUAL_INPUT";           // 手动录入
}
