package com.school.management.application.task;

import com.school.management.application.task.query.TaskDTO;
import com.school.management.common.PageResult;
import com.school.management.domain.task.model.aggregate.Task;
import com.school.management.domain.task.model.valueobject.TaskPriority;
import com.school.management.domain.task.model.valueobject.TaskStatus;
import com.school.management.domain.task.repository.TaskRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务查询服务 - CQRS查询端
 * 专门处理只读查询操作，不修改任何状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskQueryService {

    private final TaskRepository taskRepository;

    /**
     * 根据ID获取任务
     */
    public TaskDTO getById(Long id) {
        return taskRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("任务不存在: " + id));
    }

    /**
     * 根据任务编码获取任务
     */
    public TaskDTO getByTaskCode(String taskCode) {
        return taskRepository.findByTaskCode(taskCode)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("任务不存在: " + taskCode));
    }

    /**
     * 获取用户分配的任务（作为分配人）
     */
    public List<TaskDTO> findByAssignerId(Long assignerId) {
        return taskRepository.findByAssignerId(assignerId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取部门的任务
     */
    public List<TaskDTO> findByDepartmentId(Long departmentId) {
        return taskRepository.findByDepartmentId(departmentId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据状态查询任务
     */
    public List<TaskDTO> findByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户待接收的任务（作为执行人）
     */
    public List<TaskDTO> findPendingTasksForUser(Long assigneeId) {
        return taskRepository.findPendingByAssigneeId(assigneeId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户进行中的任务（作为执行人）
     */
    public List<TaskDTO> findInProgressTasksForUser(Long assigneeId) {
        return taskRepository.findInProgressByAssigneeId(assigneeId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户待审批的任务（作为审批人）
     */
    public List<TaskDTO> findPendingApprovalForUser(Long approverId) {
        return taskRepository.findPendingApprovalByApproverId(approverId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询已逾期任务
     */
    public List<TaskDTO> findOverdueTasks() {
        return taskRepository.findOverdue(LocalDateTime.now()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询任务
     */
    public PageResult<TaskDTO> findByPage(TaskQueryCriteria criteria) {
        int pageNum = criteria.getPageNum() != null ? criteria.getPageNum() : 1;
        int pageSize = criteria.getPageSize() != null ? criteria.getPageSize() : 10;

        TaskRepository.TaskQueryCriteria repoCriteria = buildRepositoryCriteria(criteria);

        List<Task> tasks = taskRepository.findByPage(repoCriteria, pageNum, pageSize);
        long total = taskRepository.countByCriteria(repoCriteria);

        List<TaskDTO> dtos = tasks.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtos, total, pageNum, pageSize);
    }

    /**
     * 统计部门任务数量
     */
    public long countByDepartmentId(Long departmentId) {
        return taskRepository.countByDepartmentId(departmentId);
    }

    /**
     * 统计各状态任务数量
     */
    public TaskStatistics getStatistics() {
        long pending = taskRepository.countByStatus(TaskStatus.PENDING);
        long inProgress = taskRepository.countByStatus(TaskStatus.IN_PROGRESS);
        long submitted = taskRepository.countByStatus(TaskStatus.SUBMITTED);
        long completed = taskRepository.countByStatus(TaskStatus.COMPLETED);
        long rejected = taskRepository.countByStatus(TaskStatus.REJECTED);
        long total = pending + inProgress + submitted + completed + rejected;

        return new TaskStatistics(pending, inProgress, submitted, completed, rejected, total);
    }

    /**
     * 检查任务是否存在
     */
    public boolean existsById(Long id) {
        return taskRepository.findById(id).isPresent();
    }

    /**
     * 检查任务编码是否存在
     */
    public boolean existsByTaskCode(String taskCode) {
        return taskRepository.existsByTaskCode(taskCode);
    }

    private TaskRepository.TaskQueryCriteria buildRepositoryCriteria(TaskQueryCriteria criteria) {
        TaskRepository.TaskQueryCriteria repoCriteria = new TaskRepository.TaskQueryCriteria();
        repoCriteria.setKeyword(criteria.getKeyword());
        repoCriteria.setAssignerId(criteria.getAssignerId());
        repoCriteria.setAssigneeId(criteria.getAssigneeId());
        repoCriteria.setDepartmentId(criteria.getDepartmentId());
        repoCriteria.setStatus(criteria.getStatus());
        repoCriteria.setPriority(criteria.getPriority());
        repoCriteria.setDueDateFrom(criteria.getStartDate());
        repoCriteria.setDueDateTo(criteria.getEndDate());
        return repoCriteria;
    }

    private TaskDTO toDTO(Task task) {
        boolean isOverdue = task.getDueDate() != null &&
                           task.getDueDate().isBefore(LocalDateTime.now()) &&
                           task.getStatus() != TaskStatus.COMPLETED;

        return TaskDTO.builder()
                .id(task.getId())
                .taskCode(task.getTaskCode())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority() != null ? task.getPriority().getCode() : null)
                .priorityName(task.getPriority() != null ? task.getPriority().getName() : null)
                .assignerId(task.getAssignerId())
                .assignerName(task.getAssignerName())
                .assignType(task.getAssignType())
                .departmentId(task.getDepartmentId())
                .dueDate(task.getDueDate())
                .status(task.getStatus() != null ? task.getStatus().getCode() : null)
                .statusName(task.getStatus() != null ? task.getStatus().getName() : null)
                .targetIds(task.getTargetIds())
                .acceptedAt(task.getAcceptedAt())
                .submittedAt(task.getSubmittedAt())
                .completedAt(task.getCompletedAt())
                .workflowTemplateId(task.getWorkflowTemplateId())
                .processInstanceId(task.getProcessInstanceId())
                .currentNode(task.getCurrentNode())
                .currentApprovers(task.getCurrentApprovers())
                .attachmentIds(task.getAttachmentIds())
                .overdue(isOverdue)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    /**
     * 查询条件
     */
    @lombok.Data
    public static class TaskQueryCriteria {
        private String keyword;
        private TaskStatus status;
        private TaskPriority priority;
        private Long departmentId;
        private Long assignerId;
        private Long assigneeId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer pageNum;
        private Integer pageSize;
    }

    /**
     * 任务统计
     */
    public record TaskStatistics(
        long pending,
        long inProgress,
        long submitted,
        long completed,
        long rejected,
        long total
    ) {}
}
