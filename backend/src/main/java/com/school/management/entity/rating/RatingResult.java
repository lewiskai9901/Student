package com.school.management.entity.rating;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 评级结果
 *
 * @author System
 * @since 4.4.0
 */
@Data
@TableName("rating_result")
public class RatingResult {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 评级配置ID
     */
    private Long ratingConfigId;

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 评级周期类型：DAILY/WEEKLY/MONTHLY
     */
    private String periodType;

    /**
     * 周期开始日期
     */
    private LocalDate periodStart;

    /**
     * 周期结束日期
     */
    private LocalDate periodEnd;

    /**
     * 排名（1=第一名）
     */
    private Integer ranking;

    /**
     * 最终得分
     */
    private BigDecimal finalScore;

    /**
     * 是否获得该评级：0否 1是
     */
    private Integer awarded;

    /**
     * 状态：DRAFT/PENDING_APPROVAL/PUBLISHED
     */
    private String status;

    /**
     * 计算时间
     */
    private LocalDateTime calculatedAt;

    /**
     * 审核人ID
     */
    private Long approvedBy;

    /**
     * 审核时间
     */
    private LocalDateTime approvedAt;

    /**
     * 发布人ID
     */
    private Long publishedBy;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

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
     * 逻辑删除：0未删除 1已删除
     */
    @TableLogic
    private Integer deleted;
}
