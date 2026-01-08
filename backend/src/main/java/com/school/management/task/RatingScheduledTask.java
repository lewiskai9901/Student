package com.school.management.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.entity.CheckPlan;
import com.school.management.mapper.CheckPlanMapper;
import com.school.management.service.rating.CheckPlanPeriodConfigService;
import com.school.management.service.rating.DailyClassSummaryService;
import com.school.management.service.rating.RatingCalculationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 评级定时任务
 *
 * @author System
 * @since 4.4.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RatingScheduledTask {

    private final CheckPlanMapper checkPlanMapper;
    private final DailyClassSummaryService dailyClassSummaryService;
    private final RatingCalculationEngine ratingCalculationEngine;
    private final CheckPlanPeriodConfigService periodConfigService;

    /**
     * 每日汇总任务（每天 23:59 执行）
     */
    @Scheduled(cron = "0 59 23 * * ?")
    public void dailySummaryTask() {
        log.info("开始执行每日汇总任务");

        try {
            LocalDate today = LocalDate.now();

            // 获取所有启用的检查计划
            LambdaQueryWrapper<CheckPlan> query = new LambdaQueryWrapper<>();
            query.eq(CheckPlan::getStatus, 1); // 启用状态
            List<CheckPlan> checkPlans = checkPlanMapper.selectList(query);

            for (CheckPlan plan : checkPlans) {
                try {
                    // 汇总当日数据
                    dailyClassSummaryService.summarizeDaily(plan.getId(), today);
                    log.info("检查计划 {} 的每日汇总完成", plan.getPlanName());
                } catch (Exception e) {
                    log.error("检查计划 {} 的每日汇总失败: {}", plan.getPlanName(), e.getMessage(), e);
                }
            }

            log.info("每日汇总任务执行完成");
        } catch (Exception e) {
            log.error("每日汇总任务执行失败", e);
        }
    }

    /**
     * 日评级计算任务（每天 23:59 执行，在汇总后）
     */
    @Scheduled(cron = "0 59 23 * * ?")
    public void dailyRatingTask() {
        log.info("开始执行日评级计算任务");

        try {
            LocalDate today = LocalDate.now();

            // 获取所有启用的检查计划
            LambdaQueryWrapper<CheckPlan> query = new LambdaQueryWrapper<>();
            query.eq(CheckPlan::getStatus, 1);
            List<CheckPlan> checkPlans = checkPlanMapper.selectList(query);

            for (CheckPlan plan : checkPlans) {
                try {
                    ratingCalculationEngine.calculateDailyRatings(plan.getId(), today);
                    log.info("检查计划 {} 的日评级计算完成", plan.getPlanName());
                } catch (Exception e) {
                    log.error("检查计划 {} 的日评级计算失败: {}", plan.getPlanName(), e.getMessage(), e);
                }
            }

            log.info("日评级计算任务执行完成");
        } catch (Exception e) {
            log.error("日评级计算任务执行失败", e);
        }
    }

    /**
     * 周评级计算任务（每周一 00:00 执行）
     */
    @Scheduled(cron = "0 0 0 ? * MON")
    public void weeklyRatingTask() {
        log.info("开始执行周评级计算任务");

        try {
            // 获取上周的周起始日期
            LocalDate lastWeekStart = LocalDate.now().minusWeeks(1);

            // 获取所有启用的检查计划
            LambdaQueryWrapper<CheckPlan> query = new LambdaQueryWrapper<>();
            query.eq(CheckPlan::getStatus, 1);
            List<CheckPlan> checkPlans = checkPlanMapper.selectList(query);

            for (CheckPlan plan : checkPlans) {
                try {
                    // 获取该计划的周起始日配置
                    int weekStartDay = periodConfigService.getWeekStartDay(plan.getId());

                    // 计算实际的周起始日期
                    DayOfWeek startDayOfWeek = DayOfWeek.of(weekStartDay);
                    LocalDate weekStart = lastWeekStart.with(TemporalAdjusters.previousOrSame(startDayOfWeek));

                    ratingCalculationEngine.calculateWeeklyRatings(plan.getId(), weekStart);
                    log.info("检查计划 {} 的周评级计算完成", plan.getPlanName());
                } catch (Exception e) {
                    log.error("检查计划 {} 的周评级计算失败: {}", plan.getPlanName(), e.getMessage(), e);
                }
            }

            log.info("周评级计算任务执行完成");
        } catch (Exception e) {
            log.error("周评级计算任务执行失败", e);
        }
    }

    /**
     * 月评级计算任务（每月1日 00:00 执行）
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void monthlyRatingTask() {
        log.info("开始执行月评级计算任务");

        try {
            // 获取上个月的年份和月份
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            int year = lastMonth.getYear();
            int month = lastMonth.getMonthValue();

            // 获取所有启用的检查计划
            LambdaQueryWrapper<CheckPlan> query = new LambdaQueryWrapper<>();
            query.eq(CheckPlan::getStatus, 1);
            List<CheckPlan> checkPlans = checkPlanMapper.selectList(query);

            for (CheckPlan plan : checkPlans) {
                try {
                    ratingCalculationEngine.calculateMonthlyRatings(plan.getId(), year, month);
                    log.info("检查计划 {} 的月评级计算完成", plan.getPlanName());
                } catch (Exception e) {
                    log.error("检查计划 {} 的月评级计算失败: {}", plan.getPlanName(), e.getMessage(), e);
                }
            }

            log.info("月评级计算任务执行完成");
        } catch (Exception e) {
            log.error("月评级计算任务执行失败", e);
        }
    }
}
