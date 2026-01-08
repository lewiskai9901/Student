package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 评级等级实体类
 * 对应表: rating_levels
 *
 * @author Claude
 * @since 2025-11-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rating_levels")
public class RatingLevel extends BaseEntity {

    /**
     * 评级规则ID
     */
    private Long ratingRuleId;

    /**
     * 等级名称: "优秀", "良好", "一般", "需改进"
     */
    private String levelName;

    /**
     * 等级编码: "EXCELLENT", "GOOD", "FAIR", "POOR"
     */
    private String levelCode;

    /**
     * 等级顺序: 1, 2, 3, 4
     */
    private Integer levelOrder;

    /**
     * 最小百分比: 0.00 (前0%)
     */
    private BigDecimal minPercent;

    /**
     * 最大百分比: 20.00 (前20%)
     */
    private BigDecimal maxPercent;

    /**
     * 最小分数: -10.00 (扣分10分以内)
     */
    private BigDecimal minScore;

    /**
     * 最大分数: 0.00 (满分)
     */
    private BigDecimal maxScore;

    /**
     * 等级颜色: "#67C23A"
     */
    private String levelColor;

    /**
     * 等级图标: "⭐⭐⭐⭐⭐"
     */
    private String levelIcon;

    /**
     * 奖励积分
     */
    private Integer rewardPoints;
}
