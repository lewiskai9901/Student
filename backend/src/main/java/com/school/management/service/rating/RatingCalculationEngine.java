package com.school.management.service.rating;

import com.school.management.entity.rating.RatingConfig;
import com.school.management.entity.rating.RatingResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 评级计算引擎
 *
 * @author System
 * @since 4.4.0
 */
public interface RatingCalculationEngine {

    /**
     * 计算指定周期的评级
     *
     * @param ratingConfig 评级配置
     * @param periodStart 周期开始日期
     * @param periodEnd 周期结束日期
     * @return 评级结果列表
     */
    List<RatingResult> calculateRating(RatingConfig ratingConfig, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 重新计算指定周期的评级（用于申诉后重算）
     *
     * @param ratingConfigId 评级配置ID
     * @param periodStart 周期开始日期
     * @param periodEnd 周期结束日期
     * @param appealId 关联的申诉ID
     * @return 评级结果列表
     */
    List<RatingResult> recalculateRating(Long ratingConfigId, LocalDate periodStart, LocalDate periodEnd, Long appealId);

    /**
     * 计算当日评级（自动触发）
     *
     * @param checkPlanId 检查计划ID
     * @param checkDate 检查日期
     */
    void calculateDailyRatings(Long checkPlanId, LocalDate checkDate);

    /**
     * 计算周评级（自动触发）
     *
     * @param checkPlanId 检查计划ID
     * @param weekStartDate 周起始日期
     */
    void calculateWeeklyRatings(Long checkPlanId, LocalDate weekStartDate);

    /**
     * 计算月评级（自动触发）
     *
     * @param checkPlanId 检查计划ID
     * @param month 月份（1-12）
     * @param year 年份
     */
    void calculateMonthlyRatings(Long checkPlanId, int year, int month);
}
