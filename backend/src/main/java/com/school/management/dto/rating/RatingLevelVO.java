package com.school.management.dto.rating;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评级等级视图对象
 *
 * @author system
 * @since 3.2.0
 */
@Data
public class RatingLevelVO {

    /**
     * 等级ID
     */
    private Long id;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 等级顺序
     */
    private Integer levelOrder;

    /**
     * 等级名称
     */
    private String levelName;

    /**
     * 等级颜色
     */
    private String levelColor;

    /**
     * 等级图标
     */
    private String levelIcon;

    /**
     * 扣分下限
     */
    private BigDecimal minScore;

    /**
     * 扣分上限
     */
    private BigDecimal maxScore;

    /**
     * 名次数量
     */
    private Integer rankCount;

    /**
     * 百分比
     */
    private BigDecimal percentage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
