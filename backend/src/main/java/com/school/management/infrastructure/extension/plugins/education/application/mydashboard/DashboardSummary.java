package com.school.management.infrastructure.extension.plugins.education.application.mydashboard;

/**
 * /my/dashboard 首页头部四个数字。
 */
public record DashboardSummary(
        int todayLessons,
        int weeklyHoursCurrent,
        int weeklyHoursTotal,
        int substituteRequests
) {}
