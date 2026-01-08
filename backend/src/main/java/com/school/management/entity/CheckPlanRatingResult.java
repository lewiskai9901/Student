package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 检查计划评级结果实体类
 *
 * @author system
 * @since 3.2.0
 */
@Data
@TableName("check_plan_rating_results")
public class CheckPlanRatingResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联规则ID
     */
    private Long ruleId;

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 检查记录ID（DAILY类型时有值）
     */
    private Long checkRecordId;

    /**
     * 检查日期（DAILY类型时有值）
     */
    private LocalDate checkDate;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称（冗余存储）
     */
    private String className;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称（冗余存储）
     */
    private String gradeName;

    /**
     * 评级等级ID
     */
    private Long levelId;

    /**
     * 等级名称（冗余存储）
     */
    private String levelName;

    /**
     * 等级顺序（冗余存储）
     */
    private Integer levelOrder;

    /**
     * 排名（扣分从少到多）
     */
    private Integer ranking;

    /**
     * 参与评级的班级总数
     */
    private Integer totalClasses;

    /**
     * 扣分（用于评级的分数）
     */
    private BigDecimal score;

    /**
     * 统计周期开始（SUMMARY类型时使用）
     */
    private LocalDate periodStart;

    /**
     * 统计周期结束（SUMMARY类型时使用）
     */
    private LocalDate periodEnd;

    /**
     * 包含的检查记录数（SUMMARY类型）
     */
    private Integer recordCount;

    /**
     * 审核状态: 0待审核 1已通过 2已驳回
     */
    private Integer approvalStatus;

    /**
     * 审核人ID
     */
    private Long approvedBy;

    /**
     * 审核时间
     */
    private LocalDateTime approvedAt;

    /**
     * 审核备注
     */
    private String approvalRemark;

    /**
     * 发布状态: 0未发布 1已发布
     */
    private Integer publishStatus;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 版本号(支持修改)
     */
    private Integer version;

    /**
     * 原始等级ID(修改前)
     */
    private Long originalLevelId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // 审核状态常量
    public static final int APPROVAL_PENDING = 0;
    public static final int APPROVAL_APPROVED = 1;
    public static final int APPROVAL_REJECTED = 2;

    // 发布状态常量
    public static final int PUBLISH_UNPUBLISHED = 0;
    public static final int PUBLISH_PUBLISHED = 1;
}
