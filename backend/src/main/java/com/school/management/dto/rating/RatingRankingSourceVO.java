package com.school.management.dto.rating;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 评级排名数据源 VO
 *
 * @author System
 * @since 4.4.0
 */
@Data
public class RatingRankingSourceVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 评级配置ID
     */
    private Long ratingConfigId;

    /**
     * 数据源类型
     */
    private String sourceType;

    /**
     * 数据源类型显示文本
     */
    private String sourceTypeText;

    /**
     * 数据源ID
     */
    private Long sourceId;

    /**
     * 数据源名称
     */
    private String sourceName;

    /**
     * 是否使用加权分：0否 1是
     */
    private Integer useWeighted;

    /**
     * 组合排名权重
     */
    private BigDecimal weight;

    /**
     * 缺失数据策略
     */
    private String missingDataStrategy;

    /**
     * 缺失数据策略显示文本
     */
    private String missingDataStrategyText;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
