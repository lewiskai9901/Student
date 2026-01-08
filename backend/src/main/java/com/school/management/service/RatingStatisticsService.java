package com.school.management.service;

import com.school.management.dto.rating.statistics.ClassFrequencyDetailVO;
import com.school.management.dto.rating.statistics.RatingFrequencyStatisticsVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 评级统计服务接口（增强版）
 *
 * 提供评级次数统计、连续记录计算、院系对比等核心功能
 * 用于评级次数统计中心页面的数据支撑
 *
 * @author Claude Code
 * @since 2025-12-22
 */
public interface RatingStatisticsService {

    /**
     * 获取评级频次综合统计（核心方法）
     *
     * 包含：
     * - 概览数据（总评级次数、获奖班级数、参评班级数）
     * - 按等级分组统计（每个等级的频次、占比、TOP班级）
     * - 院系对比统计（各院系的优秀率、排名）
     *
     * @param checkPlanId 检查计划ID
     * @param ruleId      评级规则ID
     * @param levelIds    包含的等级ID列表（可选，为空则包含所有等级）
     * @param periodStart 统计周期开始日期
     * @param periodEnd   统计周期结束日期
     * @param periodType  周期类型：WEEK/MONTH/SEMESTER/CUSTOM
     * @return 综合统计VO
     */
    RatingFrequencyStatisticsVO getFrequencyStatistics(
            Long checkPlanId,
            Long ruleId,
            List<Long> levelIds,
            LocalDate periodStart,
            LocalDate periodEnd,
            String periodType
    );

    /**
     * 按等级获取班级详细列表
     *
     * 用于展开某个等级，查看该等级下所有班级的详细信息
     * 包含：频次、占比、排名、连续记录、最近日期、徽章等
     *
     * @param levelId     等级ID
     * @param periodStart 统计周期开始日期
     * @param periodEnd   统计周期结束日期
     * @param sortBy      排序方式：frequency频次/rate占比/consecutive连续记录
     * @return 班级详细列表
     */
    List<ClassFrequencyDetailVO> getClassesByLevel(
            Long levelId,
            LocalDate periodStart,
            LocalDate periodEnd,
            String sortBy
    );

    /**
     * 计算并更新连续获得记录
     *
     * 在每次评级结果生成后调用，更新班级的连续获得次数和最佳记录
     *
     * 算法逻辑：
     * 1. 获取班级当天的评级结果
     * 2. 倒序查找历史记录，计算连续天数
     * 3. 更新frequency表的consecutive_count和best_streak字段
     *
     * @param ruleId 评级规则ID
     * @param date   评级日期
     */
    void calculateConsecutiveRecords(Long ruleId, LocalDate date);

    /**
     * 更新最近获得日期列表
     *
     * 在每次评级结果生成后调用，更新班级的最近5次获得日期
     *
     * @param ruleId  评级规则ID
     * @param levelId 等级ID
     * @param classId 班级ID
     * @param date    评级日期
     */
    void updateRecentDates(Long ruleId, Long levelId, Long classId, LocalDate date);

    /**
     * 获取连续获奖TOP榜
     *
     * 用于展示连续获得某等级最多的班级排行榜
     *
     * @param ruleId  评级规则ID
     * @param levelId 等级ID
     * @param topN    TOP数量
     * @return 连续记录TOP榜
     */
    List<ClassFrequencyDetailVO> getConsecutiveTop(Long ruleId, Long levelId, Integer topN);

    /**
     * 刷新频次统计缓存
     *
     * 用于手动触发统计数据重新计算
     *
     * @param checkPlanId 检查计划ID
     * @param periodStart 统计周期开始日期
     * @param periodEnd   统计周期结束日期
     */
    void refreshStatisticsCache(Long checkPlanId, LocalDate periodStart, LocalDate periodEnd);
}
