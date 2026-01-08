package com.school.management.dto.rating;

import lombok.Data;

/**
 * 评级统计 VO
 *
 * @author System
 * @since 4.4.0
 */
@Data
public class RatingStatisticsVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 检查计划名称
     */
    private String checkPlanName;

    /**
     * 评级配置ID
     */
    private Long ratingConfigId;

    /**
     * 评级名称
     */
    private String ratingName;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 评级周期类型
     */
    private String periodType;

    /**
     * 评级周期类型显示文本
     */
    private String periodTypeText;

    /**
     * 统计年份
     */
    private Integer year;

    /**
     * 统计月份（1-12）
     */
    private Integer month;

    /**
     * 统计期间显示文本（如：2024年、2024年1月）
     */
    private String periodText;

    /**
     * 获得该评级次数
     */
    private Integer awardedCount;
}
