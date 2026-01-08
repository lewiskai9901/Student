package com.school.management.service.rating;

import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.dto.rating.RatingStatisticsVO;
import com.school.management.entity.rating.RatingStatistics;

import java.util.List;

/**
 * 评级统计服务
 *
 * @author System
 * @since 4.4.0
 */
public interface RatingStatisticsService extends IService<RatingStatistics> {

    /**
     * 更新评级统计
     *
     * @param ratingConfigId 评级配置ID
     * @param year 年份
     * @param month 月份（可选，仅用于日评级和周评级）
     */
    void updateStatistics(Long ratingConfigId, int year, Integer month);

    /**
     * 获取班级评级统计
     *
     * @param classId 班级ID
     * @param ratingConfigId 评级配置ID（可选）
     * @param year 年份
     * @param month 月份（可选）
     * @return 统计列表
     */
    List<RatingStatisticsVO> getClassStatistics(Long classId, Long ratingConfigId, Integer year, Integer month);

    /**
     * 获取检查计划的评级统计
     *
     * @param checkPlanId 检查计划ID
     * @param year 年份
     * @param month 月份（可选）
     * @return 统计列表
     */
    List<RatingStatisticsVO> getPlanStatistics(Long checkPlanId, Integer year, Integer month);
}
