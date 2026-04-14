package com.school.management.application.my;

/**
 * /my/dashboard 首页头部四个数字。
 */
public record DashboardSummary(
        int todayLessons,
        int weeklyHoursCurrent,
        int weeklyHoursTotal,
        int substituteRequests
) {}
