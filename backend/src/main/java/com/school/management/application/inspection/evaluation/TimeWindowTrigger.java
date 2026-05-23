package com.school.management.application.inspection.evaluation;

import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.execution.ProjectStatus;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.model.scoring.TriggerMode;
import com.school.management.domain.inspection.repository.IndicatorRepository;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 时间窗触发器 (Phase 3, 2026-05-23).
 *
 * <p>对所有 {@code triggerMode==TIME_WINDOW} 的 indicator, 在周期边界自动触发评估:
 * <ul>
 *   <li>DAILY → 每日 00:05 处理"昨日"</li>
 *   <li>WEEKLY → 每周一 00:10 处理"上周"</li>
 *   <li>MONTHLY → 每月 1 号 01:15 处理"上月"</li>
 * </ul>
 *
 * <p>仅扫 status=ACTIVE 项目的 indicator. PER_TASK 由 submission 事件驱动, 不在此扫.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeWindowTrigger {

    private final IndicatorRepository indicatorRepository;
    private final InspProjectRepository projectRepository;
    private final IndicatorEvaluationService evaluationService;

    @Scheduled(cron = "0 5 0 * * *")
    public void scanDaily() {
        LocalDate target = LocalDate.now().minusDays(1);
        log.info("TimeWindowTrigger.scanDaily target={}", target);
        runForPeriod("DAILY", target);
    }

    @Scheduled(cron = "0 10 0 * * MON")
    public void scanWeekly() {
        LocalDate target = LocalDate.now().minusDays(7);
        log.info("TimeWindowTrigger.scanWeekly anchor={}", target);
        runForPeriod("WEEKLY", target);
    }

    @Scheduled(cron = "0 15 1 1 * *")
    public void scanMonthly() {
        LocalDate target = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        log.info("TimeWindowTrigger.scanMonthly anchor={}", target);
        runForPeriod("MONTHLY", target);
    }

    /** Visible for testing — 触发一个具体 evaluationPeriod 的扫描. */
    public int runForPeriod(String evaluationPeriod, LocalDate referenceDate) {
        LocalDate[] bounds = PeriodKeyResolver.boundariesFor(evaluationPeriod, referenceDate);
        String periodKey = PeriodKeyResolver.forPeriod(evaluationPeriod, bounds[0]);

        int count = 0;
        for (InspProject project : projectRepository.findByStatus(ProjectStatus.PUBLISHED)) {
            List<Indicator> indicators = indicatorRepository.findByProjectId(project.getId());
            for (Indicator ind : indicators) {
                if (ind.getTriggerMode() != TriggerMode.TIME_WINDOW) continue;
                if (!evaluationPeriod.equals(ind.getEvaluationPeriod())) continue;
                try {
                    evaluationService.evaluate(ind.getId(), periodKey, bounds[0], bounds[1]);
                    count++;
                } catch (Exception ex) {
                    log.error("TimeWindowTrigger 失败 indicator={} period={}: {}",
                            ind.getId(), periodKey, ex.getMessage(), ex);
                }
            }
        }
        log.info("TimeWindowTrigger.{} done at {} — 触发 {} 个 indicator",
                evaluationPeriod, LocalDateTime.now(), count);
        return count;
    }
}
