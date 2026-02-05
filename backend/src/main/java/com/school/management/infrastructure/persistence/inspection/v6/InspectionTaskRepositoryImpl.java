package com.school.management.infrastructure.persistence.inspection.v6;

import com.school.management.domain.inspection.model.v6.InspectionTask;
import com.school.management.domain.inspection.model.v6.TaskStatus;
import com.school.management.domain.inspection.repository.v6.InspectionTaskRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * V6检查任务仓储实现
 */
@Repository
public class InspectionTaskRepositoryImpl implements InspectionTaskRepository {

    private final InspectionTaskMapper mapper;

    public InspectionTaskRepositoryImpl(InspectionTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspectionTask save(InspectionTask task) {
        InspectionTaskPO po = toPO(task);
        if (task.getId() == null) {
            mapper.insert(po);
            task.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return task;
    }

    @Override
    public void saveAll(List<InspectionTask> tasks) {
        for (InspectionTask task : tasks) {
            save(task);
        }
    }

    @Override
    public Optional<InspectionTask> findById(Long id) {
        InspectionTaskPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<InspectionTask> findByTaskCode(String taskCode) {
        InspectionTaskPO po = mapper.findByTaskCode(taskCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<InspectionTask> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionTask> findByProjectIdAndDate(Long projectId, LocalDate taskDate) {
        return mapper.findByProjectIdAndDate(projectId, taskDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionTask> findByInspectorId(Long inspectorId) {
        return mapper.findByInspectorId(inspectorId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionTask> findByStatus(TaskStatus status) {
        return mapper.findByStatus(status.name()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionTask> findAvailableTasksForDate(LocalDate date) {
        return mapper.findAvailableTasksForDate(date).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionTask> findMyTasks(Long inspectorId) {
        return mapper.findMyTasks(inspectorId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionTask> findPagedWithConditions(int page, int size, Long projectId, TaskStatus status,
                                                         LocalDate startDate, LocalDate endDate, Long inspectorId) {
        int offset = (page - 1) * size;
        String statusStr = status != null ? status.name() : null;
        return mapper.findPagedWithConditions(offset, size, projectId, statusStr, startDate, endDate, inspectorId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countWithConditions(Long projectId, TaskStatus status, LocalDate startDate,
                                    LocalDate endDate, Long inspectorId) {
        String statusStr = status != null ? status.name() : null;
        return mapper.countWithConditions(projectId, statusStr, startDate, endDate, inspectorId);
    }

    @Override
    public boolean claimTask(Long id, Long inspectorId, String inspectorName) {
        return mapper.claimTask(id, inspectorId, inspectorName) > 0;
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void updateTotalTargets(Long id, Integer totalTargets) {
        mapper.updateTotalTargets(id, totalTargets);
    }

    @Override
    public void incrementCompletedTargets(Long id) {
        mapper.incrementCompletedTargets(id);
    }

    @Override
    public void incrementSkippedTargets(Long id) {
        mapper.incrementSkippedTargets(id);
    }

    @Override
    public int countByProjectAndDate(Long projectId, LocalDate taskDate) {
        return mapper.countByProjectAndDate(projectId, taskDate);
    }

    private InspectionTaskPO toPO(InspectionTask domain) {
        InspectionTaskPO po = new InspectionTaskPO();
        po.setId(domain.getId());
        po.setTaskCode(domain.getTaskCode());
        po.setProjectId(domain.getProjectId());
        po.setTaskDate(domain.getTaskDate());
        po.setTimeSlot(domain.getTimeSlot());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        po.setInspectorId(domain.getInspectorId());
        po.setInspectorName(domain.getInspectorName());
        po.setClaimedAt(domain.getClaimedAt());
        po.setStartedAt(domain.getStartedAt());
        po.setSubmittedAt(domain.getSubmittedAt());
        po.setReviewedAt(domain.getReviewedAt());
        po.setPublishedAt(domain.getPublishedAt());
        po.setTotalTargets(domain.getTotalTargets());
        po.setCompletedTargets(domain.getCompletedTargets());
        po.setSkippedTargets(domain.getSkippedTargets());
        po.setRemarks(domain.getRemarks());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private InspectionTask toDomain(InspectionTaskPO po) {
        return InspectionTask.builder()
                .id(po.getId())
                .taskCode(po.getTaskCode())
                .projectId(po.getProjectId())
                .taskDate(po.getTaskDate())
                .timeSlot(po.getTimeSlot())
                .status(TaskStatus.fromCode(po.getStatus()))
                .inspectorId(po.getInspectorId())
                .inspectorName(po.getInspectorName())
                .claimedAt(po.getClaimedAt())
                .startedAt(po.getStartedAt())
                .submittedAt(po.getSubmittedAt())
                .reviewedAt(po.getReviewedAt())
                .publishedAt(po.getPublishedAt())
                .totalTargets(po.getTotalTargets())
                .completedTargets(po.getCompletedTargets())
                .skippedTargets(po.getSkippedTargets())
                .remarks(po.getRemarks())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
