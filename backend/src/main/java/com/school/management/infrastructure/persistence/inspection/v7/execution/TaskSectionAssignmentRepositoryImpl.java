package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.execution.TaskSectionAssignment;
import com.school.management.domain.inspection.repository.v7.TaskSectionAssignmentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TaskSectionAssignmentRepositoryImpl implements TaskSectionAssignmentRepository {

    private final TaskSectionAssignmentMapper mapper;

    public TaskSectionAssignmentRepositoryImpl(TaskSectionAssignmentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TaskSectionAssignment save(TaskSectionAssignment assignment) {
        TaskSectionAssignmentPO po = toPO(assignment);
        if (assignment.getId() == null) {
            mapper.insert(po);
            assignment.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return assignment;
    }

    @Override
    public Optional<TaskSectionAssignment> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<TaskSectionAssignment> findByTaskId(Long taskId) {
        return mapper.findByTaskId(taskId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByTaskId(Long taskId) {
        mapper.delete(new LambdaQueryWrapper<TaskSectionAssignmentPO>()
                .eq(TaskSectionAssignmentPO::getTaskId, taskId));
    }

    private TaskSectionAssignmentPO toPO(TaskSectionAssignment d) {
        TaskSectionAssignmentPO po = new TaskSectionAssignmentPO();
        po.setId(d.getId());
        po.setTaskId(d.getTaskId());
        po.setSectionId(d.getSectionId());
        po.setInspectorId(d.getInspectorId());
        po.setStatus(d.getStatus());
        po.setStartedAt(d.getStartedAt());
        po.setCompletedAt(d.getCompletedAt());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private TaskSectionAssignment toDomain(TaskSectionAssignmentPO po) {
        return TaskSectionAssignment.reconstruct(TaskSectionAssignment.builder()
                .id(po.getId())
                .taskId(po.getTaskId())
                .sectionId(po.getSectionId())
                .inspectorId(po.getInspectorId())
                .status(po.getStatus())
                .startedAt(po.getStartedAt())
                .completedAt(po.getCompletedAt())
                .createdAt(po.getCreatedAt()));
    }
}
