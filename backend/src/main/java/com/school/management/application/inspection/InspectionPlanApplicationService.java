package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.execution.*;
import com.school.management.domain.inspection.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 检查计划应用服务.
 *
 * <p>评级引擎完美架构 (2026-05-23): 调度组不再持有 scoringProfileId; 评分配置
 * 由 ScoringProfile 按 (project, section) 自动定位, 不再由调度组持有.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class InspectionPlanApplicationService {

    private final InspectionPlanRepository planRepository;
    private final InspProjectRepository projectRepository;
    private final InspTaskRepository taskRepository;
    private final ProjectInspectorRepository projectInspectorRepository;
    private final com.school.management.infrastructure.persistence.inspection.execution.InspectionPlanInspectorMapper planInspectorMapper;

    /**
     * 2026-05-24: 校验调度组的 inspectorIds 必须全部在项目 inspector 池.
     * 空列表 (=全员可领取) 跳过校验.
     *
     * @throws IllegalArgumentException 若有 user_id 不在 project_inspector
     */
    private void validateInspectorsBelongToProject(Long projectId, java.util.List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        java.util.Set<Long> projectUserIds = projectInspectorRepository.findByProjectId(projectId).stream()
                .filter(pi -> Boolean.TRUE.equals(pi.getIsActive()))
                .map(pi -> pi.getUserId())
                .collect(java.util.stream.Collectors.toSet());
        java.util.List<Long> orphans = userIds.stream()
                .filter(uid -> !projectUserIds.contains(uid))
                .toList();
        if (!orphans.isEmpty()) {
            throw new IllegalArgumentException(
                "以下检查员不在项目检查员池中, 请先在「人员与任务」添加: " + orphans);
        }
    }

    /** smell A: 解析 assignStrategy 字符串 + 默认推导. */
    private com.school.management.domain.inspection.model.execution.AssignStrategy resolveAssignStrategy(
            String str, java.util.List<Long> userIds) {
        if (str != null && !str.isBlank()) {
            try {
                return com.school.management.domain.inspection.model.execution.AssignStrategy.valueOf(str);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("无效的 assignStrategy: " + str
                    + " (应为 SPECIFIC 或 OPEN_TO_ALL)");
            }
        }
        // 缺省: 有 inspector → SPECIFIC; 无 inspector → OPEN_TO_ALL
        return (userIds != null && !userIds.isEmpty())
                ? com.school.management.domain.inspection.model.execution.AssignStrategy.SPECIFIC
                : com.school.management.domain.inspection.model.execution.AssignStrategy.OPEN_TO_ALL;
    }

    /** 解析 JSON 串到 List&lt;Long&gt; — 用 domain 自带的 parse 经由 setter 转一道. */
    private java.util.List<Long> parseInspectorIdsJson(String json) {
        if (json == null || json.isBlank()) return java.util.Collections.emptyList();
        InspectionPlan tmp = InspectionPlan.builder().planName("__tmp").projectId(0L).build();
        tmp.updateInspectorIds(json);
        return tmp.getInspectorUserIds();
    }

    // ========== Plan CRUD ==========

    /**
     * 创建检查计划.
     */
    @Transactional
    public InspectionPlan createPlan(Long projectId, String planName, Long rootSectionId,
                                     String sectionIds, String inspectorIds,
                                     String scheduleMode, String cycleType, Integer frequency,
                                     String scheduleDays, String timeSlots, Boolean skipHolidays,
                                     Integer ratersPerTarget, String assignStrategyStr,
                                     Long createdBy) {
        // 校验项目存在且状态允许
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));
        validateProjectForPlanModification(project);

        // 若未指定模板，从项目的 rootSectionId 继承
        Long resolvedRootSectionId = rootSectionId != null ? rootSectionId : project.getRootSectionId();
        int resolvedRaters = ratersPerTarget != null ? ratersPerTarget : 1;
        validateRatersPerTarget(resolvedRaters, inspectorIds);

        // 2026-05-24: 校验所有 inspectorIds 必须属于项目 inspector 池.
        java.util.List<Long> userIds = parseInspectorIdsJson(inspectorIds);
        validateInspectorsBelongToProject(projectId, userIds);

        // smell A: 解析 assignStrategy; 缺省时从 inspectorIds 推导
        com.school.management.domain.inspection.model.execution.AssignStrategy resolvedStrategy =
                resolveAssignStrategy(assignStrategyStr, userIds);

        InspectionPlan plan = InspectionPlan.builder()
                .projectId(projectId)
                .planName(planName)
                .rootSectionId(resolvedRootSectionId)
                .sectionIds(sectionIds)
                .inspectorIds(inspectorIds)
                .assignStrategy(resolvedStrategy)
                .scheduleMode(scheduleMode)
                .cycleType(cycleType)
                .frequency(frequency)
                .scheduleDays(scheduleDays)
                .timeSlots(timeSlots)
                .skipHolidays(skipHolidays)
                .ratersPerTarget(resolvedRaters)
                .createdBy(createdBy)
                .build();

        // smell A + B: 创建前断言不变量 (assignStrategy 与 inspectorUserIds 一致 + scheduleMode 与其他字段一致)
        plan.assertAssignStrategyInvariant();
        plan.assertScheduleModeInvariant();

        InspectionPlan saved = planRepository.save(plan);
        log.info("创建检查计划: projectId={}, planName={}, rootSectionId={}, scheduleMode={}",
                projectId, planName, resolvedRootSectionId, scheduleMode);
        return saved;
    }

    /**
     * 更新检查计划.
     */
    @Transactional
    public InspectionPlan updatePlan(Long planId, String planName, Long rootSectionId,
                                     String sectionIds, String inspectorIds,
                                     String scheduleMode, String cycleType, Integer frequency,
                                     String scheduleDays, String timeSlots, Boolean skipHolidays,
                                     Integer ratersPerTarget, String assignStrategyStr) {
        InspectionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("检查计划不存在: " + planId));

        // 校验关联项目状态
        InspProject project = projectRepository.findById(plan.getProjectId())
                .orElseThrow(() -> new IllegalStateException("检查计划关联的项目不存在: " + plan.getProjectId()));
        validateProjectForPlanModification(project);

        plan.update(planName, rootSectionId, sectionIds, scheduleMode, cycleType, frequency,
                scheduleDays, timeSlots, skipHolidays, null, null);
        if (inspectorIds != null) {
            // 2026-05-24: 更新前校验 inspectorIds 全在项目 inspector 池.
            java.util.List<Long> userIds = parseInspectorIdsJson(inspectorIds);
            validateInspectorsBelongToProject(plan.getProjectId(), userIds);
            plan.updateInspectorIds(inspectorIds);
        }

        // smell A: 显式指派策略 - 优先客户端传值, 否则按 inspectorUserIds 推导
        if (assignStrategyStr != null && !assignStrategyStr.isBlank()) {
            try {
                plan.updateAssignStrategy(
                    com.school.management.domain.inspection.model.execution.AssignStrategy.valueOf(assignStrategyStr));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("无效的 assignStrategy: " + assignStrategyStr);
            }
        }

        // ratersPerTarget 为空时沿用现有值 (部分更新语义).
        int resolvedRaters = ratersPerTarget != null ? ratersPerTarget : plan.getRatersPerTarget();
        validateRatersPerTarget(resolvedRaters, plan.getInspectorIds());
        plan.updateRatersPerTarget(resolvedRaters);

        // smell A + B: 更新后断言不变量
        plan.assertAssignStrategyInvariant();
        plan.assertScheduleModeInvariant();

        InspectionPlan saved = planRepository.save(plan);
        log.info("更新检查计划: planId={}, planName={}", planId, planName);
        return saved;
    }

    /**
     * 删除检查计划
     */
    @Transactional
    public void deletePlan(Long planId) {
        InspectionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("检查计划不存在: " + planId));

        InspProject project = projectRepository.findById(plan.getProjectId())
                .orElseThrow(() -> new IllegalStateException("检查计划关联的项目不存在: " + plan.getProjectId()));
        validateProjectForPlanModification(project);

        planRepository.deleteById(planId);
        log.info("删除检查计划: planId={}", planId);
    }

    /**
     * 查询项目下的所有检查计划
     */
    @Transactional(readOnly = true)
    public List<InspectionPlan> listPlans(Long projectId) {
        return planRepository.findByProjectId(projectId);
    }

    /**
     * 获取单个检查计划
     */
    @Transactional(readOnly = true)
    public Optional<InspectionPlan> getPlan(Long planId) {
        return planRepository.findById(planId);
    }

    // ========== Plan Enable/Disable ==========

    /**
     * 启用检查计划
     */
    @Transactional
    public InspectionPlan enablePlan(Long planId) {
        InspectionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("检查计划不存在: " + planId));
        plan.enable();
        InspectionPlan saved = planRepository.save(plan);
        log.info("启用检查计划: planId={}, planName={}", planId, plan.getPlanName());
        return saved;
    }

    /**
     * 禁用检查计划
     */
    @Transactional
    public InspectionPlan disablePlan(Long planId) {
        InspectionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("检查计划不存在: " + planId));
        plan.disable();
        InspectionPlan saved = planRepository.save(plan);
        log.info("禁用检查计划: planId={}, planName={}", planId, plan.getPlanName());
        return saved;
    }

    // ========== On-Demand Trigger ==========

    /**
     * 手动触发 ON_DEMAND 类型的检查计划，创建一个任务
     */
    @Transactional
    public InspTask triggerOnDemandPlan(Long planId, Long operatorId) {
        InspectionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("检查计划不存在: " + planId));

        if (!plan.isOnDemand()) {
            throw new IllegalStateException("只有 ON_DEMAND 类型的计划才能手动触发, 当前模式: " + plan.getScheduleMode());
        }
        if (!Boolean.TRUE.equals(plan.getIsEnabled())) {
            throw new IllegalStateException("检查计划未启用, planId: " + planId);
        }

        InspProject project = projectRepository.findById(plan.getProjectId())
                .orElseThrow(() -> new IllegalStateException("检查计划关联的项目不存在: " + plan.getProjectId()));
        if (project.getStatus() != ProjectStatus.PUBLISHED) {
            throw new IllegalStateException("项目未发布，无法创建任务, 项目状态: " + project.getStatus());
        }

        // 创建任务
        String taskCode = generateTaskCode();
        InspTask task = InspTask.create(taskCode, plan.getProjectId(), LocalDate.now());

        Long assignedInspectorId = null;
        String assignedInspectorName = null;
        if (plan.getInspectorIds() != null && !plan.getInspectorIds().isBlank()) {
            try {
                var ids = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(plan.getInspectorIds(), Long[].class);
                if (ids.length > 0) {
                    assignedInspectorId = ids[0];
                    assignedInspectorName = String.valueOf(ids[0]);
                }
            } catch (Exception e) {
                log.warn("解析计划检查员列表失败: {}", e.getMessage());
            }
        }

        var builder = InspTask.builder()
                .taskCode(task.getTaskCode())
                .projectId(task.getProjectId())
                .taskDate(task.getTaskDate())
                .status(task.getStatus())
                .inspectionPlanId(planId)
                .assignedSectionIds(plan.getSectionIds())
                .createdAt(task.getCreatedAt());
        if (assignedInspectorId != null) {
            builder.inspectorId(assignedInspectorId)
                   .inspectorName(assignedInspectorName)
                   .status(TaskStatus.CLAIMED);
        }
        task = InspTask.reconstruct(builder);
        InspTask saved = taskRepository.save(task);

        log.info("手动触发 ON_DEMAND 计划: planId={}, taskCode={}, operatorId={}",
                planId, saved.getTaskCode(), operatorId);
        return saved;
    }

    // ========== Internal ==========

    /**
     * 校验 ratersPerTarget. 调度组若未指定检查员名单 (全员可领取) 不做硬校验.
     */
    private void validateRatersPerTarget(int ratersPerTarget, String inspectorIdsJson) {
        if (ratersPerTarget < 1) {
            throw new IllegalArgumentException("ratersPerTarget 必须 >= 1, 当前值: " + ratersPerTarget);
        }
        if (ratersPerTarget == 1) return;
        if (inspectorIdsJson == null || inspectorIdsJson.isBlank()) {
            return;
        }
        int available;
        try {
            Long[] ids = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(inspectorIdsJson, Long[].class);
            available = ids.length;
        } catch (Exception e) {
            log.warn("解析调度组检查员列表失败, 跳过 ratersPerTarget 人数校验: {}", e.getMessage());
            return;
        }
        if (ratersPerTarget > available) {
            throw new com.school.management.exception.BusinessException(
                    "每目标检查员份数 (" + ratersPerTarget + ") 超过调度组可用检查员数 ("
                    + available + "), 请增加检查员或调低份数");
        }
    }

    private void validateProjectForPlanModification(InspProject project) {
        ProjectStatus status = project.getStatus();
        if (status != ProjectStatus.DRAFT && status != ProjectStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "项目当前状态 " + status + " 不允许修改检查计划，仅 DRAFT 或 PUBLISHED 状态可操作");
        }
    }

    private String generateTaskCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "TSK-" + dateStr + "-" + random;
    }
}
