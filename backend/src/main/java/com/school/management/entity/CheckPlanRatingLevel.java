package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检查计划评级等级实体类
 *
 * @author system
 * @since 3.2.0
 */
@Data
@TableName("check_plan_rating_levels")
public class CheckPlanRatingLevel implements Serializable {

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
     * 等级顺序（1最高，数字越小等级越高）
     */
    private Integer levelOrder;

    /**
     * 等级名称
     */
    private String levelName;

    /**
     * 等级颜色（用于前端展示）
     */
    private String levelColor;

    /**
     * 等级图标
     */
    private String levelIcon;

    /**
     * 扣分下限（含），SCORE_RANGE时使用
     * 注意：扣分越少越好，minScore <= 实际扣分 < maxScore
     */
    private BigDecimal minScore;

    /**
     * 扣分上限（不含），SCORE_RANGE时使用
     */
    private BigDecimal maxScore;

    /**
     * 名次数量，RANK_COUNT时使用（如前3名）
     */
    private Integer rankCount;

    /**
     * 百分比，PERCENTAGE时使用（如前10%）
     */
    private BigDecimal percentage;

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
}
