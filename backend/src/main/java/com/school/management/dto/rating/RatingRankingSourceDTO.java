package com.school.management.dto.rating;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 评级排名数据源 DTO
 *
 * @author System
 * @since 4.4.0
 */
@Data
public class RatingRankingSourceDTO {

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
     * 组合排名权重（0.0-1.0，总和必须为1.0）
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
}
