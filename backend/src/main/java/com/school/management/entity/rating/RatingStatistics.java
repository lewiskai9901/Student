package com.school.management.entity.rating;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评级统计
 *
 * @author System
 * @since 4.4.0
 */
@Data
@TableName("rating_statistics")
public class RatingStatistics {

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
     * 评级配置ID
     */
    private Long ratingConfigId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 评级周期类型：DAILY/WEEKLY/MONTHLY
     */
    private String periodType;

    /**
     * 统计年份
     */
    private Integer year;

    /**
     * 统计月份（1-12）
     */
    private Integer month;

    /**
     * 获得该评级次数
     */
    private Integer awardedCount;

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
