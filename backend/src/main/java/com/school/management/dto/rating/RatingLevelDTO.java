package com.school.management.dto.rating;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 评级等级DTO
 *
 * @author system
 * @since 3.2.0
 */
@Data
public class RatingLevelDTO {

    /**
     * 等级顺序（1最高，数字越小等级越高）
     */
    @NotNull(message = "等级顺序不能为空")
    private Integer levelOrder;

    /**
     * 等级名称
     */
    @NotBlank(message = "等级名称不能为空")
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
     * 扣分下限（含），SCORE_RANGE时使用
     */
    private BigDecimal minScore;

    /**
     * 扣分上限（不含），SCORE_RANGE时使用
     */
    private BigDecimal maxScore;

    /**
     * 名次数量，RANK_COUNT时使用
     */
    private Integer rankCount;

    /**
     * 百分比，PERCENTAGE时使用
     */
    private BigDecimal percentage;
}
