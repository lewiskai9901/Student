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
 * 检查计划应用服务
 * 管理项目下的检查计划（排期），支持定期调度和手动触发。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class InspectionPlanApplicationService {

    private final InspectionPlanRepository planRepository;
    private final InspProjectRepository projectRepository;
    private final InspTaskRepository taskRepository;

    // ========== Plan CRUD ==========

    /**
     * 创建检查计划
     *
     * @param projectId     关联项目ID
     * @param planName      计划名称
     * @param rootSectionId 该计划使用的模板根分区ID（可选；若传 null 则从项目继承）
     * @param sectionIds    关联的一级分区ID列表（JSON，空则覆盖全部）
     * @param scheduleMode  调度模式: REGULAR / ON_DEMAND
     * @param cycleType     周期类型: DAILY / WEEKLY / MONTHLY
     * @param frequency     每周期执行次数
     * @param scheduleDays  调度日（JSON: [1,3,5] 表示周几）
     * @param timeSlots     时间段（JSON: ["07:00-08:00"]）
     * @param skipHolidays  是否跳过节假日
     * @param createdBy     创建人ID
     * @return 创建的检查计划
     */
    @Transactional
    public InspectionPlan createPlan(Long projectId, String planName, Long rootSectionId,
                                     String sectionIds, String inspectorIds,
                                     String scheduleMode, String cycleType, Integer frequency,
                                     String scheduleDays, String timeSlots, Boolean skipHolidays,
                                     Long scoringProfileId, Integer ratersPerTarget,
                                     Long createdBy) {
        // 校验项目存在且状态允许
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));
        validateProjectForPlanModification(project);

        // 若未指定模板，从项目的 rootSectionId 继承
        Long resolvedRootSectionId = rootSectionId != null ? rootSectionId : project.getRootSectionId();
        // 评分配置下沉: 调度组若未指定评分方案, 预填项目默认评分方案
        Long resolvedScoringProfileId = scoringProfileId != null
                ? scoringProfileId : project.getDefaultScoringProfileId();
        int resolvedRaters = ratersPerTarget != null ? ratersPerTarget : 1;
        validateRatersPerTarget(resolvedRaters, inspectorIds);

        InspectionPlan plan = InspectionPlan.builder()
                .projectId(projectId)
                .planName(planName)
                .rootSectionId(resolvedRootSectionId)
                .sectionIds(sectionIds)
                .inspectorIds(inspectorIds)
                .scheduleMode(scheduleMode)
                .cycleType(cycleType)
                .frequency(frequency)
                .scheduleDays(scheduleDays)
                .timeSlots(timeSlots)
                .skipHolidays(skipHolidays)
                .scoringProfileId(resolvedScoringProfileId)
                .ratersPerTarget(resolvedRaters)
                .createdBy(createdBy)
                .build();

        InspectionPlan saved = planRepository.save(plan);
        log.info("创建检查计划: projectId={}, planName={}, rootSectionId={}, scheduleMode={}",
                projectId, planName, resolvedRootSectionId, scheduleMode);
        return saved;
    }

    /**
     * 更新检查计划
     */
    @Transactional
    public InspectionPlan updatePlan(Long planId, String planName, Long rootSectionId,
                                     String sectionIds, String inspectorIds,
                                     String scheduleMode, String cycleType, Integer frequency,
                                     String scheduleDays, String timeSlots, Boolean skipHolidays,
                                     Long scoringProfileId, Integer ratersPerTarget) {
        InspectionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("检查计划不存在: " + planId));

        // 校验关联项目状态
        InspProject project = projectRepository.findById(plan.getProjectId())
                .orElseThrow(() -> new IllegalStateException("检查计划关联的项目不存在: " + plan.getProjectId()));
        validateProjectForPlanModification(project);

        plan.update(planName, rootSectionId, sectionIds, scheduleMode, cycleType, frequency,
                scheduleDays, timeSlots, skipHolidays, null, null);
        if (inspectorIds != null) plan.updateInspectorIds(inspectorIds);

        // 评分配置下沉: ratersPerTarget 为空时沿用现有值 (部分更新语义).
        // 用更新后的 inspectorIds 做检查员数校验.
        int resolvedRaters = ratersPerTarget != null ? ratersPerTarget : plan.getRatersPerTarget();
        validateRatersPerTarget(resolvedRaters, plan.getInspectorIds());
        plan.updateScoringConfig(scoringProfileId, resolvedRaters);

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
     *
     * @param planId     计划ID
     * @param operatorId 操作人ID
     * @return 创建的任务
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

        // 如果计划指定了检查员，自动取第一个进行指派（轮转逻辑后续可扩展）
        Long assignedInspectorId = null;
        String assignedInspectorName = null;
        if (plan.getInspectorIds() != null && !plan.getInspectorIds().isBlank()) {
            try {
                var ids = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(plan.getInspectorIds(), Long[].class);
                if (ids.length > 0) {
                    assignedInspectorId = ids[0];
                    // 简单使用 ID 作为名字占位，实际应查询用户表
                    assignedInspectorName = String.valueOf(ids[0]);
                }
            } catch (Exception e) {
                log.warn("解析计划检查员列表失败: {}", e.getMessage());
            }
        }

        // 通过 reconstruct 设置 inspectionPlanId 和 assignedSectionIds
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
     * 校验项目状态是否允许修改计划
     * 只有 DRAFT 和 PUBLISHED 状态的项目可以修改计划
     */
    /**
     * 评分配置下沉 (2026-05-23): 校验 ratersPerTarget.
     *
     * <p>规则:
     * <ul>
     *   <li>ratersPerTarget 必须 >= 1 (由 {@link InspectionPlan#updateScoringConfig} 兜底, 这里提前给友好报错)</li>
     *   <li>多人评分 (ratersPerTarget > 1) 时, 不得超过调度组指定的可用检查员数 —
     *       inspectorIds 为空表示"项目全员可领取", 此时不在调度组层硬校验人数</li>
     * </ul>
     *
     * <p>说明: 当前任务生成仍是每目标 1 份 submission (InspSubmission 无 raterId 维度,
     * 单任务只挂一名 inspector). ratersPerTarget > 1 的实际 N 份 submission 生成与
     * 多人合并由所引用 ScoringProfile.multiRaterMode 处理 — 若下游 N-rater 任务扇出
     * 与合并逻辑尚未实现, 此处只落库配置 + 校验人数, 不产生 N 份。
     */
    private void validateRatersPerTarget(int ratersPerTarget, String inspectorIdsJson) {
        if (ratersPerTarget < 1) {
            throw new IllegalArgumentException("ratersPerTarget 必须 >= 1, 当前值: " + ratersPerTarget);
        }
        if (ratersPerTarget == 1) return;
        if (inspectorIdsJson == null || inspectorIdsJson.isBlank()) {
            // 未指定检查员名单 = 项目全员可领取, 调度组层不硬校验人数
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
