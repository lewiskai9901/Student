package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评级规则实体类
 * 对应表: rating_rules
 *
 * @author Claude
 * @since 2025-11-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rating_rules")
public class RatingRule extends BaseEntity {

    /**
     * 评级模板ID
     */
    private Long ratingTemplateId;

    /**
     * 评级名称: "卫生优秀班级"
     */
    private String ratingName;

    /**
     * 评级编码: "HYGIENE_EXCELLENT"
     */
    private String ratingCode;

    /**
     * 评级描述
     */
    private String ratingDescription;

    /**
     * 评级依据: SINGLE_CATEGORY, MULTI_CATEGORY, TOTAL
     */
    private String ratingBasis;

    /**
     * 参与评级的类别ID: "1,2,3"
     */
    private String categoryIds;

    /**
     * 分数类型: DEDUCTION(原始扣分), WEIGHTED(加权后)
     */
    private String scoreType;

    /**
     * 条件类型: PERCENTAGE, ABSOLUTE, HYBRID
     */
    private String conditionType;

    /**
     * 图标: "🏆", "⭐"
     */
    private String icon;

    /**
     * 颜色: "#67C23A"
     */
    private String color;

    /**
     * 奖励积分
     */
    private Integer rewardPoints;

    /**
     * 奖励描述
     */
    private String rewardDescription;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private Integer isEnabled;
}
