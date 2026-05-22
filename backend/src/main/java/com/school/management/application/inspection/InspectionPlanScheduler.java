package com.school.management.application.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.execution.*;
import com.school.management.domain.inspection.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 检查计划定时调度器
 * 每天凌晨 6:00 扫描所有 REGULAR 类型的启用计划，
 * 判断今天是否需要生成任务，如果需要则创建任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionPlanScheduler {

    private final InspectionPlanRepository planRepository;
    private final InspProjectRepository projectRepository;
    private final InspTaskRepository taskRepository;
    private final InspTaskApplicationService taskService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 每天 06:00 执行
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void dailyScan() {
        log.info("检查计划定时扫描开始...");
        LocalDate today = LocalDate.now();
        int created = 0;
        int skipped = 0;

        // P1#11 (评估留 TODO): 此处 findAll() 全量项目 + 每项目 findEnabledByProjectId
        // 是 N+1. 当前项目体量 (数十~数百项目) 下每日一次的开销可接受, 暂不优化.
        // 若项目数增长到千级, 改为: (1) projectRepository 增加
        // findByStatus(PUBLISHED) 直接过滤; (2) 一次性 JOIN 查 enabled REGULAR
        // 计划. 同类 N+1 也存在于 EffectivenessCheckJob / IndicatorScoreService.computeLeafScore.
        List<InspProject> projects = projectRepository.findAll();
        for (InspProject project : projects) {
            if (project.getStatus() != ProjectStatus.PUBLISHED) continue;

            List<InspectionPlan> plans = planRepository.findEnabledByProjectId(project.getId());
            for (InspectionPlan plan : plans) {
                if (!"REGULAR".equals(plan.getScheduleMode())) continue;
                if (!Boolean.TRUE.equals(plan.getIsEnabled())) continue;

                if (shouldRunToday(plan, today)) {
                    try {
                        createTaskForPlan(plan, project, today);
                        created++;
                    } catch (Exception e) {
                        log.error("为计划 {} 创建任务失败: {}", plan.getPlanName(), e.getMessage());
                    }
                } else {
                    skipped++;
                }
            }
        }

        log.info("检查计划定时扫描完成: 创建 {} 个任务, 跳过 {} 个计划", created, skipped);
    }

    /**
     * 判断今天是否需要执行该计划
     */
    boolean shouldRunToday(InspectionPlan plan, LocalDate today) {
        String cycleType = plan.getCycleType();
        if ("DAILY".equals(cycleType)) {
            return true;
        }
        if ("WEEKLY".equals(cycleType)) {
            // 检查今天是星期几，与 scheduleDays 匹配
            int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Monday ... 7=Sunday
            List<Integer> scheduleDays = parseIntList(plan.getScheduleDays());
            if (scheduleDays.isEmpty()) return today.getDayOfWeek() == DayOfWeek.MONDAY; // 默认每周一
            return scheduleDays.contains(dayOfWeek);
        }
        if ("MONTHLY".equals(cycleType)) {
            int dayOfMonth = today.getDayOfMonth();
            List<Integer> scheduleDays = parseIntList(plan.getScheduleDays());
            if (scheduleDays.isEmpty()) return dayOfMonth == 1; // 默认每月1号
            return scheduleDays.contains(dayOfMonth);
        }
        return false;
    }

    /**
     * 为计划创建今天的任务
     */
    @Transactional
    void createTaskForPlan(InspectionPlan plan, InspProject project, LocalDate date) {
        // 检查今天是否已有该计划的任务（避免重复）
        List<InspTask> existingTasks = taskRepository.findByProjectIdAndTaskDate(project.getId(), date);
        boolean alreadyExists = existingTasks.stream()
                .anyMatch(t -> plan.getId().equals(t.getInspectionPlanId()));
        if (alreadyExists) {
            log.debug("计划 {} 在 {} 已有任务，跳过", plan.getPlanName(), date);
            return;
        }

        String taskCode = "TSK-" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);

        // 处理指定检查员
        Long inspectorId = null;
        String inspectorName = null;
        TaskStatus initialStatus = TaskStatus.PENDING;

        if (plan.getInspectorIds() != null && !plan.getInspectorIds().isBlank()) {
            List<Long> ids = parseLongList(plan.getInspectorIds());
            if (!ids.isEmpty()) {
                // 轮转：按日期选人
                int index = (int) (date.toEpochDay() % ids.size());
                inspectorId = ids.get(index);
                // P2#16: 取真实姓名 — 旧实现把 ID 当姓名直接展示, 用户看到一串数字.
                inspectorName = resolveUserName(inspectorId);
                initialStatus = TaskStatus.CLAIMED;
            }
        }

        InspTask task = InspTask.reconstruct(InspTask.builder()
                .taskCode(taskCode)
                .projectId(project.getId())
                .taskDate(date)
                .status(initialStatus)
                .inspectionPlanId(plan.getId())
                .assignedSectionIds(plan.getSectionIds())
                .inspectorId(inspectorId)
                .inspectorName(inspectorName));
        InspTask saved = taskRepository.save(task);

        // 填充 submissions
        try {
            taskService.repopulateSubmissions(saved.getId());
        } catch (Exception e) {
            log.warn("为任务 {} 填充 submissions 失败: {}", taskCode, e.getMessage());
        }

        log.info("定时创建任务: plan={}, taskCode={}, date={}, inspector={}",
                plan.getPlanName(), taskCode, date, inspectorName);
    }

    /**
     * P2#16: 按 userId 查真实姓名 (real_name 优先, 回退 username).
     * 查不到 / 查询失败时回退为 ID 字符串, 保证不抛 — 调度链不能因取名失败中断.
     */
    private String resolveUserName(Long userId) {
        if (userId == null) return null;
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT real_name, username FROM users WHERE id = ? AND deleted = 0", userId);
            if (!rows.isEmpty()) {
                Map<String, Object> r = rows.get(0);
                Object realName = r.get("real_name");
                if (realName != null && !realName.toString().isBlank()) return realName.toString();
                Object username = r.get("username");
                if (username != null && !username.toString().isBlank()) return username.toString();
            }
        } catch (Exception e) {
            log.warn("查询检查员 {} 姓名失败, 回退用 ID: {}", userId, e.getMessage());
        }
        return String.valueOf(userId);
    }

    private List<Integer> parseIntList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            Integer[] arr = objectMapper.readValue(json, Integer[].class);
            return Arrays.asList(arr);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Long> parseLongList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            Long[] arr = objectMapper.readValue(json, Long[].class);
            return Arrays.asList(arr);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
