package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 检查计划评级规则实体类
 *
 * @author system
 * @since 3.2.0
 */
@Data
@TableName("check_plan_rating_rules")
public class CheckPlanRatingRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联检查计划ID
     */
    private Long checkPlanId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 评级类型：DAILY-单次检查评级，SUMMARY-汇总评级
     */
    private String ruleType;

    /**
     * 评分来源：TOTAL-总分，CATEGORY-按类别
     */
    private String scoreSource;

    /**
     * 类别ID（当scoreSource=CATEGORY时使用）
     */
    private Long categoryId;

    /**
     * 类别名称（冗余存储）
     */
    private String categoryName;

    /**
     * 是否使用加权分数：0-原始扣分，1-加权扣分
     */
    private Integer useWeightedScore;

    /**
     * 划分方式：SCORE_RANGE-分数段，RANK_COUNT-名次数量，PERCENTAGE-百分比
     */
    private String divisionMethod;

    /**
     * 汇总方式（仅SUMMARY类型）：AVERAGE-平均，SUM-累加
     */
    private String summaryMethod;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 评级周期: DAILY每天 WEEKLY每周 MONTHLY每月 CUSTOM自定义
     */
    private String ratingCycle;

    /**
     * 周期配置JSON: {dayOfWeek: 1, dayOfMonth: 1, customDays: 7}
     */
    private String cycleConfig;

    /**
     * 是否需要审核: 0否 1是
     */
    private Integer requireApproval;

    /**
     * 是否自动计算: 0否 1是
     */
    private Integer autoCalculate;

    /**
     * 创建人ID
     */
    private Long createdBy;

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

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    // ========== 枚举常量 ==========

    /**
     * 评级类型枚举
     */
    public static final String RULE_TYPE_DAILY = "DAILY";
    public static final String RULE_TYPE_SUMMARY = "SUMMARY";

    /**
     * 评分来源枚举
     */
    public static final String SCORE_SOURCE_TOTAL = "TOTAL";
    public static final String SCORE_SOURCE_CATEGORY = "CATEGORY";

    /**
     * 划分方式枚举
     */
    public static final String DIVISION_SCORE_RANGE = "SCORE_RANGE";
    public static final String DIVISION_RANK_COUNT = "RANK_COUNT";
    public static final String DIVISION_PERCENTAGE = "PERCENTAGE";

    /**
     * 汇总方式枚举
     */
    public static final String SUMMARY_AVERAGE = "AVERAGE";
    public static final String SUMMARY_SUM = "SUM";

    /**
     * 评级周期枚举
     */
    public static final String CYCLE_DAILY = "DAILY";
    public static final String CYCLE_WEEKLY = "WEEKLY";
    public static final String CYCLE_MONTHLY = "MONTHLY";
    public static final String CYCLE_CUSTOM = "CUSTOM";
}
