package com.school.management.application.inspection.v6;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.v6.*;
import com.school.management.domain.inspection.repository.v6.InspectionProjectRepository;
import com.school.management.domain.inspection.repository.v6.InspectionTaskRepository;
import com.school.management.domain.inspection.repository.v6.InspectionTargetRepository;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * V6任务生成引擎
 * 负责根据项目配置自动生成检查任务和目标
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class TaskGenerationService {

    private final InspectionProjectRepository projectRepository;
    private final InspectionTaskRepository taskRepository;
    private final InspectionTargetRepository targetRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final UniversalPlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 定时任务：每天凌晨2点生成当天的任务
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledTaskGeneration() {
        LocalDate today = LocalDate.now();
        log.info("开始生成 {} 的检查任务", today);

        List<InspectionProject> activeProjects = projectRepository.findActiveProjectsForDate(today);
        for (InspectionProject project : activeProjects) {
            try {
                generateTasksForProject(project, today);
            } catch (Exception e) {
                log.error("项目 {} 任务生成失败: {}", project.getProjectCode(), e.getMessage(), e);
            }
        }

        log.info("任务生成完成，共处理 {} 个项目", activeProjects.size());
    }

    /**
     * 手动触发：为指定项目生成指定日期的任务
     */
    @Transactional
    public List<InspectionTask> generateTasksForProject(Long projectId, LocalDate date) {
        InspectionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new IllegalStateException("只有进行中的项目才能生成任务");
        }

        return generateTasksForProject(project, date);
    }

    /**
     * 批量生成：为指定项目生成日期范围内的任务
     */
    @Transactional
    public List<InspectionTask> generateTasksForDateRange(Long projectId, LocalDate startDate, LocalDate endDate) {
        InspectionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new IllegalStateException("只有进行中的项目才能生成任务");
        }

        List<InspectionTask> allTasks = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            if (shouldGenerateTaskForDate(project, current)) {
                List<InspectionTask> tasks = generateTasksForProject(project, current);
                allTasks.addAll(tasks);
            }
            current = current.plusDays(1);
        }

        return allTasks;
    }

    /**
     * 核心方法：为项目生成指定日期的任务
     */
    private List<InspectionTask> generateTasksForProject(InspectionProject project, LocalDate date) {
        // 检查是否应该在此日期生成任务
        if (!shouldGenerateTaskForDate(project, date)) {
            log.debug("项目 {} 在 {} 不需要生成任务", project.getProjectCode(), date);
            return Collections.emptyList();
        }

        // 检查是否已有任务
        int existingCount = taskRepository.countByProjectAndDate(project.getId(), date);
        if (existingCount > 0) {
            log.debug("项目 {} 在 {} 已有 {} 个任务", project.getProjectCode(), date, existingCount);
            return Collections.emptyList();
        }

        // 获取时间段配置
        List<String> timeSlots = parseTimeSlots(project.getTimeSlots());
        if (timeSlots.isEmpty()) {
            timeSlots = Collections.singletonList("全天");
        }

        List<InspectionTask> tasks = new ArrayList<>();

        // 为每个时间段生成任务
        for (String timeSlot : timeSlots) {
            InspectionTask task = createTask(project, date, timeSlot);
            task = taskRepository.save(task);

            // 生成检查目标
            List<InspectionTarget> targets = generateTargets(project, task);
            if (!targets.isEmpty()) {
                targetRepository.saveAll(targets);
                taskRepository.updateTotalTargets(task.getId(), targets.size());
            }

            tasks.add(task);
        }

        // 更新项目任务总数
        projectRepository.updateTotalTasks(project.getId(),
                (project.getTotalTasks() != null ? project.getTotalTasks() : 0) + tasks.size());

        log.info("为项目 {} 在 {} 生成了 {} 个任务", project.getProjectCode(), date, tasks.size());

        return tasks;
    }

    /**
     * 判断是否应该在指定日期生成任务
     */
    private boolean shouldGenerateTaskForDate(InspectionProject project, LocalDate date) {
        // 检查日期范围
        if (date.isBefore(project.getStartDate())) {
            return false;
        }
        if (project.getEndDate() != null && date.isAfter(project.getEndDate())) {
            return false;
        }

        // 检查是否跳过节假日（简化实现，只跳过周末）
        if (project.isSkipHolidays()) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                return false;
            }
        }

        // 检查排除日期
        Set<LocalDate> excludedDates = parseExcludedDates(project.getExcludedDates());
        if (excludedDates.contains(date)) {
            return false;
        }

        // 根据周期类型检查
        CycleType cycleType = project.getCycleType();
        if (cycleType == null || cycleType == CycleType.DAILY) {
            return true;
        }

        if (cycleType == CycleType.WEEKLY) {
            Set<DayOfWeek> weekDays = parseCycleConfigForWeekly(project.getCycleConfig());
            return weekDays.contains(date.getDayOfWeek());
        }

        if (cycleType == CycleType.MONTHLY) {
            Set<Integer> monthDays = parseCycleConfigForMonthly(project.getCycleConfig());
            return monthDays.contains(date.getDayOfMonth());
        }

        return true;
    }

    /**
     * 创建任务实体
     */
    private InspectionTask createTask(InspectionProject project, LocalDate date, String timeSlot) {
        String taskCode = generateTaskCode(project.getProjectCode(), date, timeSlot);

        return InspectionTask.create(
                taskCode,
                project.getId(),
                date,
                timeSlot,
                project.getCreatedBy()
        );
    }

    /**
     * 生成检查目标
     */
    private List<InspectionTarget> generateTargets(InspectionProject project, InspectionTask task) {
        ScopeType scopeType = project.getScopeType();
        List<Long> scopeIds = parseScopeConfig(project.getScopeConfig());

        if (scopeIds.isEmpty()) {
            log.warn("项目 {} 没有配置检查范围", project.getProjectCode());
            return Collections.emptyList();
        }

        TargetType targetType = scopeType == ScopeType.ORG ? TargetType.ORG :
                (scopeType == ScopeType.PLACE ? TargetType.PLACE : TargetType.USER);

        List<InspectionTarget> targets = new ArrayList<>();

        for (Long scopeId : scopeIds) {
            String targetName = resolveTargetName(scopeType, scopeId);
            String targetCode = null;
            Long orgUnitId = null;
            String orgUnitName = null;

            if (scopeType == ScopeType.ORG) {
                orgUnitId = scopeId;
                orgUnitName = targetName;
            }

            InspectionTarget target = InspectionTarget.create(
                    task.getId(),
                    targetType,
                    scopeId,
                    targetName,
                    targetCode,
                    orgUnitId, orgUnitName,
                    null, null, // classId, className
                    new BigDecimal("100.00"),
                    new BigDecimal("100.00")
            );
            targets.add(target);
        }

        return targets;
    }

    /**
     * 根据范围类型查询目标真实名称
     */
    private String resolveTargetName(ScopeType scopeType, Long scopeId) {
        try {
            switch (scopeType) {
                case ORG:
                    return orgUnitRepository.findById(scopeId)
                            .map(OrgUnit::getUnitName)
                            .orElse("组织-" + scopeId);
                case PLACE:
                    return placeRepository.findById(scopeId)
                            .map(UniversalPlace::getPlaceName)
                            .orElse("场所-" + scopeId);
                case USER:
                    return userRepository.findById(scopeId)
                            .map(User::getRealName)
                            .orElse("用户-" + scopeId);
                default:
                    return "目标-" + scopeId;
            }
        } catch (Exception e) {
            log.warn("查询目标名称失败: scopeType={}, scopeId={}", scopeType, scopeId, e);
            return "目标-" + scopeId;
        }
    }

    // ========== 辅助解析方法 ==========

    private List<String> parseTimeSlots(String timeSlots) {
        if (timeSlots == null || timeSlots.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(timeSlots, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析时间段配置失败: {}", timeSlots);
            return Collections.emptyList();
        }
    }

    private Set<LocalDate> parseExcludedDates(String excludedDates) {
        if (excludedDates == null || excludedDates.isBlank()) {
            return Collections.emptySet();
        }
        try {
            List<String> dateStrings = objectMapper.readValue(excludedDates, new TypeReference<List<String>>() {});
            return dateStrings.stream()
                    .map(s -> LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("解析排除日期失败: {}", excludedDates);
            return Collections.emptySet();
        }
    }

    private Set<DayOfWeek> parseCycleConfigForWeekly(String cycleConfig) {
        if (cycleConfig == null || cycleConfig.isBlank()) {
            return EnumSet.allOf(DayOfWeek.class);
        }
        try {
            Map<String, Object> config = objectMapper.readValue(cycleConfig, new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            List<Integer> days = (List<Integer>) config.get("weekDays");
            if (days == null) {
                return EnumSet.allOf(DayOfWeek.class);
            }
            return days.stream()
                    .map(DayOfWeek::of)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("解析周期配置失败: {}", cycleConfig);
            return EnumSet.allOf(DayOfWeek.class);
        }
    }

    private Set<Integer> parseCycleConfigForMonthly(String cycleConfig) {
        if (cycleConfig == null || cycleConfig.isBlank()) {
            return Collections.emptySet();
        }
        try {
            Map<String, Object> config = objectMapper.readValue(cycleConfig, new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            List<Integer> days = (List<Integer>) config.get("monthDays");
            if (days == null) {
                return Collections.emptySet();
            }
            return new HashSet<>(days);
        } catch (Exception e) {
            log.warn("解析月度周期配置失败: {}", cycleConfig);
            return Collections.emptySet();
        }
    }

    private List<Long> parseScopeConfig(String scopeConfig) {
        if (scopeConfig == null || scopeConfig.isBlank()) {
            return Collections.emptyList();
        }
        try {
            // 尝试解析为简单数组格式 [1, 2, 3]
            return objectMapper.readValue(scopeConfig, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            // 尝试解析为对象格式 {"targetIds": [1, 2, 3]}
            try {
                Map<String, Object> config = objectMapper.readValue(scopeConfig, new TypeReference<Map<String, Object>>() {});
                Object targetIds = config.get("targetIds");
                if (targetIds instanceof List) {
                    return ((List<?>) targetIds).stream()
                            .map(id -> {
                                if (id instanceof Number) {
                                    return ((Number) id).longValue();
                                }
                                return Long.parseLong(id.toString());
                            })
                            .collect(Collectors.toList());
                }
            } catch (Exception ex) {
                log.warn("解析范围配置失败: {}", scopeConfig);
            }
            return Collections.emptyList();
        }
    }

    private String generateTaskCode(String projectCode, LocalDate date, String timeSlot) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String slotCode = timeSlot.hashCode() > 0 ?
                String.format("%02d", Math.abs(timeSlot.hashCode()) % 100) : "00";
        return "TSK" + projectCode.substring(3) + dateStr + slotCode;
    }
}
