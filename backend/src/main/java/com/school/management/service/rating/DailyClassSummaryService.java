package com.school.management.service.rating;

import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.rating.DailyClassSummary;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日班级汇总服务
 *
 * @author System
 * @since 4.4.0
 */
public interface DailyClassSummaryService extends IService<DailyClassSummary> {

    /**
     * 汇总指定日期的检查结果
     *
     * @param checkPlanId 检查计划ID
     * @param summaryDate 汇总日期
     */
    void summarizeDaily(Long checkPlanId, LocalDate summaryDate);

    /**
     * 重新汇总指定日期的检查结果（用于申诉后重算）
     *
     * @param checkPlanId 检查计划ID
     * @param summaryDate 汇总日期
     */
    void resynchronizeDaily(Long checkPlanId, LocalDate summaryDate);

    /**
     * 获取指定周期的每日汇总
     *
     * @param checkPlanId 检查计划ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日汇总列表
     */
    List<DailyClassSummary> getDailySummaries(Long checkPlanId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取指定班级在指定周期的每日汇总
     *
     * @param checkPlanId 检查计划ID
     * @param classId 班级ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日汇总列表
     */
    List<DailyClassSummary> getClassDailySummaries(Long checkPlanId, Long classId, LocalDate startDate, LocalDate endDate);
}
