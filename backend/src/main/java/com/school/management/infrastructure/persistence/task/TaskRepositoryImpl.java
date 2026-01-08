package com.school.management.infrastructure.persistence.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.task.model.aggregate.Task;
import com.school.management.domain.task.model.valueobject.TaskPriority;
import com.school.management.domain.task.model.valueobject.TaskStatus;
import com.school.management.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务仓储实现
 */
@Repository("taskDomainRepository")
@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepository {

    private final TaskDomainMapper taskMapper;

    @Override
    public Task save(Task task) {
        TaskPO po = toPO(task);
        if (po.getId() == null) {
            taskMapper.insert(po);
        } else {
            taskMapper.updateById(po);
        }
        task.setId(po.getId());
        return task;
    }

    @Override
    public Optional<Task> findById(Long id) {
        TaskPO po = taskMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void delete(Task task) {
        taskMapper.deleteById(task.getId());
    }

    @Override
    public Optional<Task> findByTaskCode(String taskCode) {
        TaskPO po = taskMapper.selectByTaskCode(taskCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Task> findByAssignerId(Long assignerId) {
        return taskMapper.selectByAssignerId(assignerId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByDepartmentId(Long departmentId) {
        return taskMapper.selectByDepartmentId(departmentId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByStatus(TaskStatus status) {
        return taskMapper.selectByStatus(status.getCode()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findPendingByAssigneeId(Long assigneeId) {
        LambdaQueryWrapper<TaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskPO::getStatus, TaskStatus.PENDING.getCode());
        wrapper.apply("JSON_CONTAINS(target_ids, CAST({0} AS JSON))", assigneeId);
        wrapper.orderByDesc(TaskPO::getCreatedAt);

        return taskMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findInProgressByAssigneeId(Long assigneeId) {
        LambdaQueryWrapper<TaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskPO::getStatus, TaskStatus.IN_PROGRESS.getCode());
        wrapper.apply("JSON_CONTAINS(target_ids, CAST({0} AS JSON))", assigneeId);
        wrapper.orderByDesc(TaskPO::getCreatedAt);

        return taskMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findPendingApprovalByApproverId(Long approverId) {
        LambdaQueryWrapper<TaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskPO::getStatus, TaskStatus.SUBMITTED.getCode());
        wrapper.apply("JSON_CONTAINS(current_approvers, CAST({0} AS JSON))", approverId);
        wrapper.orderByDesc(TaskPO::getCreatedAt);

        return taskMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findOverdue(LocalDateTime dueDate) {
        return taskMapper.selectOverdue(dueDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTaskCode(String taskCode) {
        return taskMapper.countByTaskCode(taskCode) > 0;
    }

    @Override
    public long countByDepartmentId(Long departmentId) {
        return taskMapper.countByDepartmentId(departmentId);
    }

    @Override
    public long countByStatus(TaskStatus status) {
        return taskMapper.countByStatus(status.getCode());
    }

    @Override
    public List<Task> findByPage(TaskQueryCriteria criteria, int pageNum, int pageSize) {
        Page<TaskPO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TaskPO> wrapper = buildQueryWrapper(criteria);
        wrapper.orderByDesc(TaskPO::getCreatedAt);

        Page<TaskPO> result = taskMapper.selectPage(page, wrapper);
        return result.getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByCriteria(TaskQueryCriteria criteria) {
        LambdaQueryWrapper<TaskPO> wrapper = buildQueryWrapper(criteria);
        return taskMapper.selectCount(wrapper);
    }

    private LambdaQueryWrapper<TaskPO> buildQueryWrapper(TaskQueryCriteria criteria) {
        LambdaQueryWrapper<TaskPO> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(criteria.getKeyword())) {
            wrapper.and(w -> w
                    .like(TaskPO::getTaskCode, criteria.getKeyword())
                    .or()
                    .like(TaskPO::getTitle, criteria.getKeyword())
            );
        }

        if (criteria.getAssignerId() != null) {
            wrapper.eq(TaskPO::getAssignerId, criteria.getAssignerId());
        }

        if (criteria.getDepartmentId() != null) {
            wrapper.eq(TaskPO::getDepartmentId, criteria.getDepartmentId());
        }

        if (criteria.getStatus() != null) {
            wrapper.eq(TaskPO::getStatus, criteria.getStatus().getCode());
        }

        if (criteria.getPriority() != null) {
            wrapper.eq(TaskPO::getPriority, criteria.getPriority().getCode());
        }

        if (criteria.getDueDateFrom() != null) {
            wrapper.ge(TaskPO::getDueDate, criteria.getDueDateFrom());
        }

        if (criteria.getDueDateTo() != null) {
            wrapper.le(TaskPO::getDueDate, criteria.getDueDateTo());
        }

        if (Boolean.TRUE.equals(criteria.getOverdue())) {
            wrapper.lt(TaskPO::getDueDate, LocalDateTime.now());
            wrapper.notIn(TaskPO::getStatus, TaskStatus.COMPLETED.getCode(), TaskStatus.REJECTED.getCode());
        }

        return wrapper;
    }

    private TaskPO toPO(Task task) {
        TaskPO po = new TaskPO();
        po.setId(task.getId());
        po.setTaskCode(task.getTaskCode());
        po.setTitle(task.getTitle());
        po.setDescription(task.getDescription());
        po.setPriority(task.getPriority() != null ? task.getPriority().getCode() : null);
        po.setAssignerId(task.getAssignerId());
        po.setAssignerName(task.getAssignerName());
        po.setAssignType(task.getAssignType());
        po.setDepartmentId(task.getDepartmentId());
        po.setDueDate(task.getDueDate());
        po.setStatus(task.getStatus() != null ? task.getStatus().getCode() : null);
        po.setTargetIds(task.getTargetIds());
        po.setAcceptedAt(task.getAcceptedAt());
        po.setSubmittedAt(task.getSubmittedAt());
        po.setCompletedAt(task.getCompletedAt());
        po.setWorkflowTemplateId(task.getWorkflowTemplateId());
        po.setProcessInstanceId(task.getProcessInstanceId());
        po.setCurrentNode(task.getCurrentNode());
        po.setCurrentApprovers(task.getCurrentApprovers());
        po.setAttachmentIds(task.getAttachmentIds());
        po.setVersion(task.getVersion() != null ? task.getVersion().intValue() : null);
        po.setCreatedAt(task.getCreatedAt());
        po.setUpdatedAt(task.getUpdatedAt());
        return po;
    }

    private Task toDomain(TaskPO po) {
        return Task.reconstruct(
                po.getId(),
                po.getTaskCode(),
                po.getTitle(),
                po.getDescription(),
                po.getPriority() != null ? TaskPriority.fromCode(po.getPriority()) : null,
                po.getAssignerId(),
                po.getAssignerName(),
                po.getAssignType(),
                po.getDepartmentId(),
                po.getDueDate(),
                po.getStatus() != null ? TaskStatus.fromCode(po.getStatus()) : null,
                po.getTargetIds(),
                po.getAcceptedAt(),
                po.getSubmittedAt(),
                po.getCompletedAt(),
                po.getWorkflowTemplateId(),
                po.getProcessInstanceId(),
                po.getCurrentNode(),
                po.getCurrentApprovers(),
                po.getAttachmentIds(),
                po.getVersion(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }
}
