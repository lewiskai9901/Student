package com.school.management.infrastructure.persistence.inspection.execution;

import com.school.management.domain.inspection.model.execution.AssignStrategy;
import com.school.management.domain.inspection.model.execution.InspectionPlan;
import com.school.management.domain.inspection.repository.InspectionPlanRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InspectionPlanRepositoryImpl implements InspectionPlanRepository {

    private final InspectionPlanMapper mapper;
    private final InspectionPlanInspectorMapper inspectorMapper;
    private final InspectionPlanSectionMapper sectionMapper;

    public InspectionPlanRepositoryImpl(InspectionPlanMapper mapper,
                                         InspectionPlanInspectorMapper inspectorMapper,
                                         InspectionPlanSectionMapper sectionMapper) {
        this.mapper = mapper;
        this.inspectorMapper = inspectorMapper;
        this.sectionMapper = sectionMapper;
    }

    @Override
    @Transactional
    public InspectionPlan save(InspectionPlan plan) {
        InspectionPlanPO po = toPO(plan);
        if (plan.getId() == null) {
            mapper.insert(po);
            plan.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        // V20260524_2: 同步 insp_plan_inspectors 关系表 — 全删全插, 简单可靠.
        syncInspectorRelations(plan);
        // V20260524_4 smell C: 同步 insp_plan_sections 关系表.
        syncSectionRelations(plan);
        return plan;
    }

    private void syncSectionRelations(InspectionPlan plan) {
        Long planId = plan.getId();
        sectionMapper.hardDeleteByPlanId(planId);
        List<Long> sectionIds = plan.getSectionIdList();
        if (sectionIds == null || sectionIds.isEmpty()) return;
        LinkedHashSet<Long> dedup = new LinkedHashSet<>(sectionIds);
        LocalDateTime now = LocalDateTime.now();
        for (Long sid : dedup) {
            if (sid == null) continue;
            InspectionPlanSectionPO row = new InspectionPlanSectionPO();
            row.setPlanId(planId);
            row.setSectionId(sid);
            row.setTenantId(plan.getTenantId() != null ? plan.getTenantId() : 0L);
            row.setCreatedAt(now);
            row.setDeleted(0);
            sectionMapper.insert(row);
        }
    }

    private void syncInspectorRelations(InspectionPlan plan) {
        Long planId = plan.getId();
        inspectorMapper.hardDeleteByPlanId(planId);
        List<Long> userIds = plan.getInspectorUserIds();
        if (userIds == null || userIds.isEmpty()) return;
        // 去重保序
        LinkedHashSet<Long> dedup = new LinkedHashSet<>(userIds);
        LocalDateTime now = LocalDateTime.now();
        for (Long uid : dedup) {
            if (uid == null) continue;
            InspectionPlanInspectorPO row = new InspectionPlanInspectorPO();
            row.setPlanId(planId);
            row.setUserId(uid);
            row.setTenantId(plan.getTenantId() != null ? plan.getTenantId() : 0L);
            row.setCreatedAt(now);
            row.setDeleted(0);
            inspectorMapper.insert(row);
        }
    }

    @Override
    public Optional<InspectionPlan> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<InspectionPlan> findByProjectId(Long projectId) {
        return loadWithInspectors(mapper.findByProjectId(projectId));
    }

    @Override
    public List<InspectionPlan> findEnabledByProjectId(Long projectId) {
        return loadWithInspectors(mapper.findEnabledByProjectId(projectId));
    }

    /** 批量加载: 一次性查所有 plan 的 inspector 关系, 内存分组消除 N+1. */
    private List<InspectionPlan> loadWithInspectors(List<InspectionPlanPO> pos) {
        if (pos == null || pos.isEmpty()) return new ArrayList<>();
        List<Long> planIds = pos.stream().map(InspectionPlanPO::getId).collect(Collectors.toList());
        java.util.Map<Long, List<Long>> inspectorsByPlan = new java.util.HashMap<>();
        for (InspectionPlanInspectorMapper.PlanInspectorRow row : inspectorMapper.findByPlanIds(planIds)) {
            inspectorsByPlan.computeIfAbsent(row.getPlanId(), k -> new ArrayList<>()).add(row.getUserId());
        }
        java.util.Map<Long, List<Long>> sectionsByPlan = new java.util.HashMap<>();
        for (InspectionPlanSectionMapper.PlanSectionRow row : sectionMapper.findByPlanIds(planIds)) {
            sectionsByPlan.computeIfAbsent(row.getPlanId(), k -> new ArrayList<>()).add(row.getSectionId());
        }
        return pos.stream()
                .map(po -> toDomainWith(po, inspectorsByPlan.get(po.getId()), sectionsByPlan.get(po.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        mapper.deleteById(id);
        inspectorMapper.hardDeleteByPlanId(id);
        sectionMapper.hardDeleteByPlanId(id);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        // 注: 关系表的清理交给 mapper.deleteByProjectId 或后续显式调用.
        // 这里保持原行为 (软删 plans), 关系行通过 plan_id 索引仍可追溯,
        // 后续 V20260524_3 (drop column) 时再彻底物理清理孤儿关系.
        mapper.deleteByProjectId(projectId);
    }

    private InspectionPlanPO toPO(InspectionPlan d) {
        InspectionPlanPO po = new InspectionPlanPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setProjectId(d.getProjectId());
        po.setPlanName(d.getPlanName());
        po.setRootSectionId(d.getRootSectionId());
        // V20260524_4 smell C: 不再写 section_ids JSON 列 (关系移至 insp_plan_sections)
        po.setSectionIds(null);
        po.setScheduleMode(d.getScheduleMode());
        po.setCycleType(d.getCycleType());
        po.setFrequency(d.getFrequency());
        po.setScheduleDays(d.getScheduleDays());
        po.setTimeSlots(d.getTimeSlots());
        po.setSkipHolidays(d.getSkipHolidays());
        // V20260524_2: 不再写 inspector_ids JSON 列 (关系移至 insp_plan_inspectors).
        // 旧列暂保留 NULL, V20260524_3 future 整体 DROP.
        po.setInspectorIds(null);
        po.setAssignStrategy(d.getAssignStrategy() != null ? d.getAssignStrategy().name() : AssignStrategy.OPEN_TO_ALL.name());
        po.setRatersPerTarget(d.getRatersPerTarget());
        po.setIsEnabled(d.getIsEnabled());
        po.setSortOrder(d.getSortOrder());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private InspectionPlan toDomain(InspectionPlanPO po) {
        // 单查路径: 主动加载关系
        List<Long> userIds = inspectorMapper.findUserIdsByPlanId(po.getId());
        List<Long> sectionIds = sectionMapper.findSectionIdsByPlanId(po.getId());
        return toDomainWith(po, userIds, sectionIds);
    }

    private InspectionPlan toDomainWith(InspectionPlanPO po, List<Long> userIds, List<Long> sectionIds) {
        return InspectionPlan.reconstruct(InspectionPlan.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .projectId(po.getProjectId())
                .planName(po.getPlanName())
                .rootSectionId(po.getRootSectionId())
                .sectionIdList(sectionIds != null ? sectionIds : new ArrayList<>())
                .scheduleMode(po.getScheduleMode())
                .cycleType(po.getCycleType())
                .frequency(po.getFrequency())
                .scheduleDays(po.getScheduleDays())
                .timeSlots(po.getTimeSlots())
                .skipHolidays(po.getSkipHolidays())
                .inspectorUserIds(userIds != null ? userIds : new ArrayList<>())
                .assignStrategy(po.getAssignStrategy() != null
                        ? AssignStrategy.valueOf(po.getAssignStrategy())
                        : null)
                .ratersPerTarget(po.getRatersPerTarget())
                .isEnabled(po.getIsEnabled())
                .sortOrder(po.getSortOrder())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
