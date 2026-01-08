package com.school.management.service;

import com.school.management.dto.DashboardStatisticsResponse;

public interface DashboardService {

    /**
     * 获取仪表盘统计数据
     * @param days 图表数据天数（7/30/90）
     * @return 统计数据
     */
    DashboardStatisticsResponse getStatistics(Integer days);
}
