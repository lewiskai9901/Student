package com.school.management.entity.rating;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评级排名数据源
 *
 * @author System
 * @since 4.4.0
 */
@Data
@TableName("rating_ranking_source")
public class RatingRankingSource {

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
     * 数据源类型：TOTAL_SCORE/CATEGORY/DEDUCTION_ITEM
     */
    private String sourceType;

    /**
     * 数据源ID（分类ID或扣分项ID）
     */
    private Long sourceId;

    /**
     * 是否使用加权分：0否 1是
     */
    private Integer useWeighted;

    /**
     * 组合排名权重（0.0-1.0）
     */
    private BigDecimal weight;

    /**
     * 缺失数据策略：ZERO/SKIP
     */
    private String missingDataStrategy;

    /**
     * 排序序号
     */
    private Integer sortOrder;

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
