package com.school.management.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 区间扣分配置 - 量化1.0版本
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class ScoreRange {

    /**
     * 区间最小值
     */
    private BigDecimal min;

    /**
     * 区间最大值
     */
    private BigDecimal max;

    /**
     * 该区间对应的扣分分数
     */
    private BigDecimal score;

    /**
     * 区间描述（可选）
     */
    private String description;
}
