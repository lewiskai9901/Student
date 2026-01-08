package com.school.management.service;

import com.school.management.dto.statistics.*;

import java.util.List;

/**
 * 检查计划统计服务接口
 */
public interface CheckPlanStatisticsService {

    /**
     * 获取统计概览
     * @param filters 筛选条件
     * @return 统计概览
     */
    PlanStatisticsOverviewVO getOverview(StatisticsFilters filters);

    /**
     * 获取班级排名
     * @param filters 筛选条件
     * @return 班级排名列表
     */
    List<ClassRankingVO> getClassRanking(StatisticsFilters filters);

    /**
     * 获取类别统计
     * @param filters 筛选条件
     * @return 类别统计列表
     */
    List<CategoryStatisticsVO> getCategoryStatistics(StatisticsFilters filters);

    /**
     * 获取扣分项统计
     * @param filters 筛选条件
     * @return 扣分项统计列表
     */
    List<ItemStatisticsVO> getItemStatistics(StatisticsFilters filters);

    /**
     * 获取趋势数据
     * @param filters 筛选条件
     * @return 趋势数据
     */
    TrendDataVO getTrendData(StatisticsFilters filters);

    /**
     * 导出统计报表
     * @param filters 筛选条件
     * @return 导出文件路径
     */
    String exportStatistics(StatisticsFilters filters);
}
