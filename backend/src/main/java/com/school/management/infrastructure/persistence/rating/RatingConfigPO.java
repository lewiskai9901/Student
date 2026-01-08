package com.school.management.infrastructure.persistence.rating;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评价配置持久化对象
 */
@Data
@TableName("rating_configs")
public class RatingConfigPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 评价名称
     */
    private String ratingName;

    /**
     * 周期类型: DAILY, WEEKLY, MONTHLY, SEMESTER
     */
    private String periodType;

    /**
     * 划分方式: RANKING, PERCENTAGE, THRESHOLD
     */
    private String divisionMethod;

    /**
     * 划分值
     */
    private BigDecimal divisionValue;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否需要审批
     */
    private Boolean requireApproval;

    /**
     * 是否自动发布
     */
    private Boolean autoPublish;

    /**
     * 是否启用
     */
    private Boolean enabled;

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
     * 创建人
     */
    private Long createdBy;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;
}
