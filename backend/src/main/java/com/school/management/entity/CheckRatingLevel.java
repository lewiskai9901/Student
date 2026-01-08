package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检查评级等级实体类
 * 注意: 该表没有deleted字段,所以不继承BaseEntity
 *
 * @author system
 * @since 3.1.0
 */
@Data
@TableName("check_rating_levels")
public class CheckRatingLevel {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 配置ID
     */
    private Long configId;

    /**
     * 等级名称
     */
    private String levelName;

    /**
     * 等级顺序
     */
    private Integer levelOrder;

    /**
     * 最小百分比(比例方式)
     */
    private BigDecimal minPercent;

    /**
     * 最大百分比(比例方式)
     */
    private BigDecimal maxPercent;

    /**
     * 最小分数(分数段方式)
     */
    private BigDecimal minScore;

    /**
     * 最大分数(分数段方式)
     */
    private BigDecimal maxScore;

    /**
     * 等级颜色
     */
    private String levelColor;

    /**
     * 奖励积分
     */
    private Integer rewardPoints;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
