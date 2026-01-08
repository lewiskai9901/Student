package com.school.management.dto.rating;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 评级频次视图对象
 */
@Data
public class RatingFrequencyVO {

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
     * 班级名称
     */
    private String className;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 评级规则ID
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 等级ID
     */
    private Long levelId;

    /**
     * 等级名称
     */
    private String levelName;

    /**
     * 等级颜色
     */
    private String levelColor;

    /**
     * 等级顺序
     */
    private Integer levelOrder;

    /**
     * 获得该等级的次数
     */
    private Integer frequency;

    /**
     * 参与评级的总次数
     */
    private Integer totalRatings;

    /**
     * 频次占比(%)
     */
    private BigDecimal frequencyRate;

    /**
     * 排名（按频次）
     */
    private Integer ranking;

    /**
     * 周期类型
     */
    private String periodType;

    /**
     * 周期开始日期
     */
    private LocalDate periodStart;

    /**
     * 周期结束日期
     */
    private LocalDate periodEnd;

    /**
     * 周期标签
     */
    private String periodLabel;

    /**
     * 最近一次获得该等级的日期
     */
    private LocalDate lastRatingDate;
}
