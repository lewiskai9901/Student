package com.school.management.service;

/**
 * 日常检查转换服务接口
 * 将已结束的daily_checks转换为inspection_sessions
 *
 * @author system
 * @since 2.0.0
 */
public interface DailyCheckConversionService {

    /**
     * 将已结束的日常检查转换为检查记录
     *
     * @param dailyCheckId 日常检查ID
     * @return 转换后的检查批次ID
     */
    Long convertDailyCheckToInspectionSession(Long dailyCheckId);

    /**
     * 批量转换所有已结束但未转换的日常检查
     *
     * @return 转换的数量
     */
    Integer convertAllCompletedDailyChecks();

    /**
     * 检查日常检查是否已经转换
     *
     * @param dailyCheckId 日常检查ID
     * @return 是否已转换
     */
    boolean isAlreadyConverted(Long dailyCheckId);
}
