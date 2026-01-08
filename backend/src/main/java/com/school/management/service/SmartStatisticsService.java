package com.school.management.service;

import com.school.management.dto.statistics.smart.*;

/**
 * 智能统计服务接口
 * 提供动态适应、多维度分析的统计功能
 */
public interface SmartStatisticsService {

    /**
     * 获取智能统计概览
     * 包含覆盖率、趋势、警告等智能分析结果
     *
     * @param filters 筛选条件
     * @return 智能统计概览
     */
    SmartStatisticsOverviewVO getSmartOverview(SmartStatisticsFilters filters);

    /**
     * 获取智能班级排名
     * 支持多维度排名、缺检处理、轮次归一化
     *
     * @param filters 筛选条件
     * @return 智能排名结果（包含排名列表和元信息）
     */
    SmartRankingResultVO getSmartRanking(SmartStatisticsFilters filters);

    /**
     * 获取动态类别统计
     * 自动识别检查涉及的类别并统计
     *
     * @param filters 筛选条件
     * @return 动态类别统计
     */
    DynamicCategoryStatsVO getDynamicCategoryStats(SmartStatisticsFilters filters);

    /**
     * 获取轮次分析
     * 分析多轮检查之间的变化情况
     *
     * @param filters 筛选条件
     * @return 轮次分析结果
     */
    RoundAnalysisVO getRoundAnalysis(SmartStatisticsFilters filters);

    /**
     * 获取班级追踪数据
     * 追踪单个班级在某计划下的历史表现
     *
     * @param planId 计划ID
     * @param classId 班级ID
     * @return 班级追踪数据
     */
    ClassTrackingVO getClassTracking(Long planId, Long classId);

    /**
     * 获取检查覆盖率
     *
     * @param planId 计划ID
     * @param filters 筛选条件（可选）
     * @return 覆盖率信息
     */
    CheckCoverageVO getCheckCoverage(Long planId, SmartStatisticsFilters filters);

    /**
     * 生成统计洞察
     * 自动分析数据并生成文字洞察
     *
     * @param filters 筛选条件
     * @return 洞察列表
     */
    java.util.List<String> generateInsights(SmartStatisticsFilters filters);
}
