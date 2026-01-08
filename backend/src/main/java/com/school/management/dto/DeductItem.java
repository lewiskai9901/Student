package com.school.management.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 扣分项 - 量化1.0版本
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class DeductItem {

    /**
     * 扣分项名称
     */
    private String name;

    /**
     * 扣分项编码
     */
    private String code;

    /**
     * 固定扣分分数（模式1使用）
     */
    private BigDecimal score;

    /**
     * 基础扣分分数（模式2使用）
     */
    private BigDecimal baseScore;

    /**
     * 每人扣分分数（模式2使用）
     */
    private BigDecimal perPersonScore;

    /**
     * 区间扣分配置（模式3使用）
     */
    private List<ScoreRange> ranges;
}
