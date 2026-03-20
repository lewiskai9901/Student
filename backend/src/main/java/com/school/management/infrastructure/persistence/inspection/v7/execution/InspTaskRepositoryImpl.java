package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.repository.v7.InspTaskRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InspTaskRepositoryImpl implements InspTaskRepository {

    private final InspTaskMapper mapper;

    public InspTaskRepositoryImpl(InspTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspTask save(InspTask task) {
        InspTaskPO po = toPO(task);
        if (task.getId() == null) {
            mapper.insert(po);
            task.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return task;
    }

    @Override
    public Optional<InspTask> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<InspTask> findByTaskCode(String taskCode) {
        return Optional.ofNullable(mapper.findByTaskCode(taskCode)).map(this::toDomain);
    }

    @Override
    public List<InspTask> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspTask> findByInspectorId(Long inspectorId) {
        return mapper.findByInspectorId(inspectorId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspTask> findByProjectIdAndTaskDate(Long projectId, LocalDate taskDate) {
        return mapper.findByProjectIdAndTaskDate(projectId, taskDate).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspTask> findByStatus(TaskStatus status) {
        return mapper.findByStatus(status.name()).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspTask> findAll() {
        return mapper.findAllTasks().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspTask> findAvailableTasks() {
        return mapper.findAvailableTasks().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        mapper.delete(new LambdaQueryWrapper<InspTaskPO>().eq(InspTaskPO::getProjectId, projectId));
    }

    private InspTaskPO toPO(InspTask d) {
        InspTaskPO po = new InspTaskPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setTaskCode(d.getTaskCode());
        po.setProjectId(d.getProjectId());
        po.setTaskDate(d.getTaskDate());
        po.setTimeSlotCode(d.getTimeSlotCode());
        po.setTimeSlotStart(d.getTimeSlotStart());
        po.setTimeSlotEnd(d.getTimeSlotEnd());
        po.setInspectorId(d.getInspectorId());
        po.setInspectorName(d.getInspectorName());
        po.setReviewerId(d.getReviewerId());
        po.setReviewerName(d.getReviewerName());
        po.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        po.setTotalTargets(d.getTotalTargets());
        po.setCompletedTargets(d.getCompletedTargets());
        po.setSkippedTargets(d.getSkippedTargets());
        po.setSubmittedAt(d.getSubmittedAt());
        po.setReviewedAt(d.getReviewedAt());
        po.setPublishedAt(d.getPublishedAt());
        po.setReviewComment(d.getReviewComment());
        po.setCollaborationMode(d.getCollaborationMode());
        po.setExecutionStartedAt(d.getExecutionStartedAt());
        po.setExecutionEndedAt(d.getExecutionEndedAt());
        po.setAssignedSectionIds(d.getAssignedSectionIds());
        po.setAssignedTargetIds(d.getAssignedTargetIds());
        po.setInspectionPlanId(d.getInspectionPlanId());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private InspTask toDomain(InspTaskPO po) {
        return InspTask.reconstruct(InspTask.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .taskCode(po.getTaskCode())
                .projectId(po.getProjectId())
                .taskDate(po.getTaskDate())
                .timeSlotCode(po.getTimeSlotCode())
                .timeSlotStart(po.getTimeSlotStart())
                .timeSlotEnd(po.getTimeSlotEnd())
                .inspectorId(po.getInspectorId())
                .inspectorName(po.getInspectorName())
                .reviewerId(po.getReviewerId())
                .reviewerName(po.getReviewerName())
                .status(po.getStatus() != null ? TaskStatus.valueOf(po.getStatus()) : null)
                .totalTargets(po.getTotalTargets())
                .completedTargets(po.getCompletedTargets())
                .skippedTargets(po.getSkippedTargets())
                .submittedAt(po.getSubmittedAt())
                .reviewedAt(po.getReviewedAt())
                .publishedAt(po.getPublishedAt())
                .reviewComment(po.getReviewComment())
                .collaborationMode(po.getCollaborationMode())
                .executionStartedAt(po.getExecutionStartedAt())
                .executionEndedAt(po.getExecutionEndedAt())
                .assignedSectionIds(po.getAssignedSectionIds())
                .assignedTargetIds(po.getAssignedTargetIds())
                .inspectionPlanId(po.getInspectionPlanId())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
