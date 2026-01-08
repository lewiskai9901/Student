package com.school.management.entity.rating;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评级计算明细
 *
 * @author System
 * @since 4.4.0
 */
@Data
@TableName("rating_calculation_detail")
public class RatingCalculationDetail {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 评级结果ID
     */
    private Long ratingResultId;

    /**
     * 每日汇总ID
     */
    private Long dailySummaryId;

    /**
     * 汇总日期
     */
    private java.time.LocalDate summaryDate;

    /**
     * 该日得分
     */
    private BigDecimal dayScore;

    /**
     * 该日加权得分
     */
    private BigDecimal dayWeightedScore;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
