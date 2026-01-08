package com.school.management.entity.rating;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评级配置
 *
 * @author System
 * @since 4.4.0
 */
@Data
@TableName("rating_config")
public class RatingConfig {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 评级名称（如：优秀班级、卫生班级）
     */
    private String ratingName;

    /**
     * 评级周期类型：DAILY/WEEKLY/MONTHLY
     */
    private String ratingType;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色（十六进制）
     */
    private String color;

    /**
     * 显示优先级（数字越小越靠前）
     */
    private Integer priority;

    /**
     * 划分方式：TOP_N/TOP_PERCENT/BOTTOM_N/BOTTOM_PERCENT
     */
    private String divisionMethod;

    /**
     * 划分值（3名或10%）
     */
    private BigDecimal divisionValue;

    /**
     * 是否需要审核：0否 1是
     */
    private Integer requireApproval;

    /**
     * 审核通过后自动发布：0否 1是
     */
    private Integer autoPublish;

    /**
     * 是否启用：0否 1是
     */
    private Integer enabled;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 规则说明
     */
    private String description;

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
     * 逻辑删除：0未删除 1已删除
     */
    @TableLogic
    private Integer deleted;
}
