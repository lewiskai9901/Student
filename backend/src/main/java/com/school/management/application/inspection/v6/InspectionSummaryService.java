package com.school.management.application.inspection.v6;

import com.school.management.domain.inspection.model.v6.*;
import com.school.management.domain.inspection.repository.v6.InspectionProjectRepository;
import com.school.management.domain.inspection.repository.v6.InspectionTargetRepository;
import com.school.management.domain.inspection.repository.v6.InspectionTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * V6检查汇总服务
 */
@Service
public class InspectionSummaryService {

    private static final Logger log = LoggerFactory.getLogger(InspectionSummaryService.class);

    private final InspectionProjectRepository projectRepository;
    private final InspectionTaskRepository taskRepository;
    private final InspectionTargetRepository targetRepository;
    private final JdbcTemplate jdbcTemplate;

    public InspectionSummaryService(InspectionProjectRepository projectRepository,
                                     InspectionTaskRepository taskRepository,
                                     InspectionTargetRepository targetRepository,
                                     JdbcTemplate jdbcTemplate) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.targetRepository = targetRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 定时任务：每天凌晨3点生成前一天的汇总
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledSummaryGeneration() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始生成 {} 的检查汇总", yesterday);

        List<InspectionProject> activeProjects = projectRepository.findByStatus(ProjectStatus.ACTIVE);
        for (InspectionProject project : activeProjects) {
            try {
                generateDailySummary(project.getId(), yesterday);
            } catch (Exception e) {
                log.error("项目 {} 汇总生成失败: {}", project.getProjectCode(), e.getMessage(), e);
            }
        }

        log.info("汇总生成完成，共处理 {} 个项目", activeProjects.size());
    }

    /**
     * 生成指定日期的日汇总
     */
    @Transactional
    public void generateDailySummary(Long projectId, LocalDate date) {
        // 获取该日期发布的任务
        List<InspectionTask> tasks = taskRepository.findByProjectIdAndDate(projectId, date);
        List<InspectionTask> publishedTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.PUBLISHED)
                .collect(Collectors.toList());

        if (publishedTasks.isEmpty()) {
            log.debug("项目 {} 在 {} 没有已发布的任务", projectId, date);
            return;
        }

        // 汇总每个目标的分数
        Map<String, TargetSummary> summaries = new HashMap<>();

        for (InspectionTask task : publishedTasks) {
            List<InspectionTarget> targets = targetRepository.findByTaskId(task.getId());
            for (InspectionTarget target : targets) {
                if (target.getStatus() != TargetStatus.COMPLETED) {
                    continue;
                }

                String key = target.getTargetType().name() + "_" + target.getTargetId();
                TargetSummary summary = summaries.computeIfAbsent(key, k -> new TargetSummary(target));
                summary.addInspection(target);
            }
        }

        // 删除已有的汇总
        jdbcTemplate.update(
                "DELETE FROM inspection_daily_summaries WHERE project_id = ? AND summary_date = ?",
                projectId, date);

        // 插入新汇总
        for (TargetSummary summary : summaries.values()) {
            insertDailySummary(projectId, date, summary);
        }

        // 更新排名
        updateDailyRanking(projectId, date);

        log.info("项目 {} 在 {} 生成了 {} 条汇总记录", projectId, date, summaries.size());
    }

    /**
     * 生成周汇总
     */
    @Transactional
    public void generateWeeklySummary(Long projectId, int year, int weekNumber) {
        WeekFields weekFields = WeekFields.of(Locale.CHINA);
        LocalDate startOfWeek = LocalDate.of(year, 1, 1)
                .with(weekFields.weekOfYear(), weekNumber)
                .with(weekFields.dayOfWeek(), 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        // 汇总周内每日数据
        String sql = "INSERT INTO inspection_weekly_summaries " +
                "(project_id, year, week_number, start_date, end_date, target_type, target_id, target_name, " +
                "org_unit_id, class_id, average_score, total_deduction, total_bonus, inspection_days, perfect_days) " +
                "SELECT project_id, ?, ?, ?, ?, target_type, target_id, target_name, org_unit_id, class_id, " +
                "AVG(final_score), SUM(total_deduction), SUM(total_bonus), COUNT(*), SUM(CASE WHEN total_deduction = 0 THEN 1 ELSE 0 END) " +
                "FROM inspection_daily_summaries " +
                "WHERE project_id = ? AND summary_date BETWEEN ? AND ? " +
                "GROUP BY project_id, target_type, target_id, target_name, org_unit_id, class_id " +
                "ON DUPLICATE KEY UPDATE " +
                "average_score = VALUES(average_score), total_deduction = VALUES(total_deduction), " +
                "total_bonus = VALUES(total_bonus), inspection_days = VALUES(inspection_days), " +
                "perfect_days = VALUES(perfect_days), updated_at = NOW()";

        jdbcTemplate.update(sql, year, weekNumber, startOfWeek, endOfWeek, projectId, startOfWeek, endOfWeek);

        // 更新周排名
        updateWeeklyRanking(projectId, year, weekNumber);
    }

    /**
     * 生成月汇总
     */
    @Transactional
    public void generateMonthlySummary(Long projectId, int year, int month) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        // 汇总月内每日数据
        String sql = "INSERT INTO inspection_monthly_summaries " +
                "(project_id, year, month, target_type, target_id, target_name, org_unit_id, class_id, " +
                "average_score, total_deduction, total_bonus, inspection_days, perfect_days) " +
                "SELECT project_id, ?, ?, target_type, target_id, target_name, org_unit_id, class_id, " +
                "AVG(final_score), SUM(total_deduction), SUM(total_bonus), COUNT(*), SUM(CASE WHEN total_deduction = 0 THEN 1 ELSE 0 END) " +
                "FROM inspection_daily_summaries " +
                "WHERE project_id = ? AND summary_date BETWEEN ? AND ? " +
                "GROUP BY project_id, target_type, target_id, target_name, org_unit_id, class_id " +
                "ON DUPLICATE KEY UPDATE " +
                "average_score = VALUES(average_score), total_deduction = VALUES(total_deduction), " +
                "total_bonus = VALUES(total_bonus), inspection_days = VALUES(inspection_days), " +
                "perfect_days = VALUES(perfect_days), updated_at = NOW()";

        jdbcTemplate.update(sql, year, month, projectId, startOfMonth, endOfMonth);

        // 更新月排名
        updateMonthlyRanking(projectId, year, month);
    }

    /**
     * 获取日排名
     */
    public List<Map<String, Object>> getDailyRanking(Long projectId, LocalDate date, String targetType) {
        String sql = "SELECT * FROM inspection_daily_summaries " +
                "WHERE project_id = ? AND summary_date = ? " +
                (targetType != null ? "AND target_type = ? " : "") +
                "ORDER BY daily_rank";

        if (targetType != null) {
            return jdbcTemplate.queryForList(sql, projectId, date, targetType);
        } else {
            return jdbcTemplate.queryForList(sql, projectId, date);
        }
    }

    /**
     * 获取组织汇总
     */
    public List<Map<String, Object>> getOrgSummary(Long projectId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT org_unit_id, org_unit_name, " +
                "COUNT(DISTINCT summary_date) as days, " +
                "AVG(final_score) as avg_score, " +
                "SUM(total_deduction) as total_deduction, " +
                "SUM(total_bonus) as total_bonus " +
                "FROM inspection_daily_summaries " +
                "WHERE project_id = ? AND summary_date BETWEEN ? AND ? AND org_unit_id IS NOT NULL " +
                "GROUP BY org_unit_id, org_unit_name " +
                "ORDER BY avg_score DESC";

        return jdbcTemplate.queryForList(sql, projectId, startDate, endDate);
    }

    /**
     * 获取班级汇总
     */
    public List<Map<String, Object>> getClassSummary(Long projectId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT class_id, class_name, org_unit_id, org_unit_name, " +
                "COUNT(DISTINCT summary_date) as days, " +
                "AVG(final_score) as avg_score, " +
                "SUM(total_deduction) as total_deduction, " +
                "SUM(total_bonus) as total_bonus " +
                "FROM inspection_daily_summaries " +
                "WHERE project_id = ? AND summary_date BETWEEN ? AND ? AND class_id IS NOT NULL " +
                "GROUP BY class_id, class_name, org_unit_id, org_unit_name " +
                "ORDER BY avg_score DESC";

        return jdbcTemplate.queryForList(sql, projectId, startDate, endDate);
    }

    /**
     * 获取趋势数据
     */
    public List<Map<String, Object>> getTrend(Long projectId, Long targetId, String targetType,
                                               LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT summary_date, final_score, total_deduction, total_bonus, daily_rank " +
                "FROM inspection_daily_summaries " +
                "WHERE project_id = ? AND target_type = ? AND target_id = ? " +
                "AND summary_date BETWEEN ? AND ? " +
                "ORDER BY summary_date";

        return jdbcTemplate.queryForList(sql, projectId, targetType, targetId, startDate, endDate);
    }

    // ========== 私有方法 ==========

    private void insertDailySummary(Long projectId, LocalDate date, TargetSummary summary) {
        String sql = "INSERT INTO inspection_daily_summaries " +
                "(project_id, summary_date, target_type, target_id, target_name, " +
                "org_unit_id, org_unit_name, class_id, class_name, " +
                "base_score, total_deduction, total_bonus, final_score, weighted_score, " +
                "inspection_count, deduction_count, bonus_count) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                projectId, date,
                summary.targetType.name(), summary.targetId, summary.targetName,
                summary.orgUnitId, summary.orgUnitName, summary.classId, summary.className,
                summary.baseScore, summary.totalDeduction, summary.totalBonus,
                summary.finalScore, summary.weightedScore,
                summary.inspectionCount, summary.deductionCount, summary.bonusCount);
    }

    private void updateDailyRanking(Long projectId, LocalDate date) {
        // 使用1224排名（相同分数相同排名）
        String sql = "UPDATE inspection_daily_summaries ds " +
                "JOIN (" +
                "  SELECT id, DENSE_RANK() OVER (ORDER BY final_score DESC) as ranking " +
                "  FROM inspection_daily_summaries " +
                "  WHERE project_id = ? AND summary_date = ?" +
                ") ranked ON ds.id = ranked.id " +
                "SET ds.daily_rank = ranked.ranking";

        jdbcTemplate.update(sql, projectId, date);

        // 计算排名变化
        LocalDate previousDate = date.minusDays(1);
        String updateRankChange = "UPDATE inspection_daily_summaries curr " +
                "LEFT JOIN inspection_daily_summaries prev " +
                "ON curr.project_id = prev.project_id " +
                "AND curr.target_type = prev.target_type " +
                "AND curr.target_id = prev.target_id " +
                "AND prev.summary_date = ? " +
                "SET curr.rank_change = IFNULL(prev.daily_rank, curr.daily_rank) - curr.daily_rank " +
                "WHERE curr.project_id = ? AND curr.summary_date = ?";

        jdbcTemplate.update(updateRankChange, previousDate, projectId, date);
    }

    private void updateWeeklyRanking(Long projectId, int year, int weekNumber) {
        String sql = "UPDATE inspection_weekly_summaries ws " +
                "JOIN (" +
                "  SELECT id, DENSE_RANK() OVER (ORDER BY average_score DESC) as ranking " +
                "  FROM inspection_weekly_summaries " +
                "  WHERE project_id = ? AND year = ? AND week_number = ?" +
                ") ranked ON ws.id = ranked.id " +
                "SET ws.weekly_rank = ranked.ranking";

        jdbcTemplate.update(sql, projectId, year, weekNumber);
    }

    private void updateMonthlyRanking(Long projectId, int year, int month) {
        String sql = "UPDATE inspection_monthly_summaries ms " +
                "JOIN (" +
                "  SELECT id, DENSE_RANK() OVER (ORDER BY average_score DESC) as ranking " +
                "  FROM inspection_monthly_summaries " +
                "  WHERE project_id = ? AND year = ? AND month = ?" +
                ") ranked ON ms.id = ranked.id " +
                "SET ms.monthly_rank = ranked.ranking";

        jdbcTemplate.update(sql, projectId, year, month);
    }

    // ========== 内部类 ==========

    private static class TargetSummary {
        TargetType targetType;
        Long targetId;
        String targetName;
        Long orgUnitId;
        String orgUnitName;
        Long classId;
        String className;
        BigDecimal baseScore = new BigDecimal("100.00");
        BigDecimal totalDeduction = BigDecimal.ZERO;
        BigDecimal totalBonus = BigDecimal.ZERO;
        BigDecimal finalScore = BigDecimal.ZERO;
        BigDecimal weightedScore = BigDecimal.ZERO;
        int inspectionCount = 0;
        int deductionCount = 0;
        int bonusCount = 0;

        TargetSummary(InspectionTarget target) {
            this.targetType = target.getTargetType();
            this.targetId = target.getTargetId();
            this.targetName = target.getTargetName();
            this.orgUnitId = target.getOrgUnitId();
            this.orgUnitName = target.getOrgUnitName();
            this.classId = target.getClassId();
            this.className = target.getClassName();
        }

        void addInspection(InspectionTarget target) {
            inspectionCount++;
            if (target.getDeductionTotal() != null && target.getDeductionTotal().compareTo(BigDecimal.ZERO) > 0) {
                totalDeduction = totalDeduction.add(target.getDeductionTotal());
                deductionCount++;
            }
            if (target.getBonusTotal() != null && target.getBonusTotal().compareTo(BigDecimal.ZERO) > 0) {
                totalBonus = totalBonus.add(target.getBonusTotal());
                bonusCount++;
            }

            // 计算最终分数（取平均）
            BigDecimal score = target.getFinalScore() != null ? target.getFinalScore() : baseScore;
            if (inspectionCount == 1) {
                finalScore = score;
            } else {
                finalScore = finalScore.multiply(new BigDecimal(inspectionCount - 1))
                        .add(score)
                        .divide(new BigDecimal(inspectionCount), 2, RoundingMode.HALF_UP);
            }

            weightedScore = finalScore.multiply(target.getWeightRatio() != null ?
                    target.getWeightRatio() : new BigDecimal("100.00"))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
    }
}
