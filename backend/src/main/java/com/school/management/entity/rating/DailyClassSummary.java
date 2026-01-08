package com.school.management.entity.rating;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日班级汇总
 *
 * @author System
 * @since 4.4.0
 */
@Data
@TableName("daily_class_summary")
public class DailyClassSummary {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 汇总日期
     */
    private LocalDate summaryDate;

    /**
     * 总分（未加权）
     */
    private BigDecimal totalScore;

    /**
     * 加权总分
     */
    private BigDecimal weightedTotalScore;

    /**
     * 分类得分JSON：{"category_id": {"score": 85.5, "weighted_score": 42.75}}
     */
    private String categoryScores;

    /**
     * 扣分项得分JSON：{"item_id": {"score": 90.0, "weighted_score": 18.0}}
     */
    private String itemScores;

    /**
     * 当日检查次数
     */
    private Integer checkCount;

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
