package com.school.management.application.task;

import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.domain.task.model.*;
import com.school.management.domain.task.repository.TaskRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application service for Task management.
 *
 * <p>This service orchestrates use cases related to tasks, including:
 * <ul>
 *   <li>Creating and assigning tasks</li>
 *   <li>Accepting and submitting task work</li>
 *   <li>Reviewing and approving submissions</li>
 *   <li>Cancelling tasks</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskApplicationService {

    private final TaskRepository taskRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates a new task with assignees.
     *
     * @param command the create command
     * @return the created task
     */
    @Transactional
    public Task createTask(CreateTaskCommand command) {
        log.info("Creating task: {}", command.getTitle());

        // Create the task aggregate
        Task task = Task.create(
            command.getTitle(),
            command.getDescription(),
            command.getPriority(),
            command.getCreatedBy(),
            command.getCreatedByName(),
            command.getDepartmentId(),
            command.getDueDate(),
            command.getWorkflowTemplateId(),
            command.getAttachmentIds(),
            command.getCreatedBy()
        );

        // Assign to users
        if (command.getAssignees() != null && !command.getAssignees().isEmpty()) {
            List<Task.UserAssignment> assignments = command.getAssignees().stream()
                .map(a -> new Task.UserAssignment(
                    a.getUserId(), a.getUserName(),
                    a.getDepartmentId(), a.getDepartmentName()))
                .collect(Collectors.toList());
            
            task.assignToUsers(assignments);
        }

        // Save and publish events
        task = taskRepository.save(task);
        publishEvents(task);

        log.info("Task created: {} ({})", task.getTaskCode(), task.getId());
        return task;
    }

    /**
     * Accepts a task assignment.
     *
     * @param taskId   the task ID
     * @param userId   the accepting user ID
     * @param userName the accepting user name
     * @return the updated task
     */
    @Transactional
    public Task acceptTask(Long taskId, Long userId, String userName) {
        log.info("User {} accepting task {}", userId, taskId);

        Task task = getTaskOrThrow(taskId);
        task.accept(userId, userName);

        task = taskRepository.save(task);
        publishEvents(task);

        return task;
    }

    /**
     * Submits task work for review.
     *
     * @param command the submit command
     * @return the created submission
     */
    @Transactional
    public Submission submitTask(SubmitTaskCommand command) {
        log.info("User {} submitting task {}", command.getSubmittedBy(), command.getTaskId());

        Task task = getTaskOrThrow(command.getTaskId());
        Submission submission = task.submit(
            command.getSubmittedBy(),
            command.getSubmittedByName(),
            command.getContent(),
            command.getAttachmentIds()
        );

        taskRepository.save(task);
        publishEvents(task);

        return submission;
    }

    /**
     * Approves a task submission.
     *
     * @param command the approve command
     * @return the updated task
     */
    @Transactional
    public Task approveTask(ApproveTaskCommand command) {
        log.info("Reviewer {} approving task {} for assignee {}",
            command.getReviewerId(), command.getTaskId(), command.getAssigneeUserId());

        Task task = getTaskOrThrow(command.getTaskId());
        task.approve(
            command.getAssigneeUserId(),
            command.getReviewerId(),
            command.getReviewerName(),
            command.getComment()
        );

        task = taskRepository.save(task);
        publishEvents(task);

        return task;
    }

    /**
     * Rejects a task submission.
     *
     * @param command the reject command
     * @return the updated task
     */
    @Transactional
    public Task rejectTask(RejectTaskCommand command) {
        log.info("Reviewer {} rejecting task {} for assignee {}",
            command.getReviewerId(), command.getTaskId(), command.getAssigneeUserId());

        Task task = getTaskOrThrow(command.getTaskId());
        task.reject(
            command.getAssigneeUserId(),
            command.getReviewerId(),
            command.getReviewerName(),
            command.getReason()
        );

        task = taskRepository.save(task);
        publishEvents(task);

        return task;
    }

    /**
     * Cancels a task.
     *
     * @param taskId      the task ID
     * @param reason      cancellation reason
     * @param cancelledBy user who cancelled
     * @return the cancelled task
     */
    @Transactional
    public Task cancelTask(Long taskId, String reason, Long cancelledBy) {
        log.info("Cancelling task {} by user {}", taskId, cancelledBy);

        Task task = getTaskOrThrow(taskId);
        task.cancel(reason, cancelledBy);

        task = taskRepository.save(task);
        publishEvents(task);

        return task;
    }

    // Query methods

    /**
     * Gets a task by ID.
     *
     * @param taskId the task ID
     * @return the task or empty
     */
    @Transactional(readOnly = true)
    public Optional<Task> getTask(Long taskId) {
        return taskRepository.findById(taskId);
    }

    /**
     * Gets a task by task code.
     *
     * @param taskCode the task code
     * @return the task or empty
     */
    @Transactional(readOnly = true)
    public Optional<Task> getTaskByCode(String taskCode) {
        return taskRepository.findByTaskCode(taskCode);
    }

    /**
     * Gets tasks assigned to a user.
     *
     * @param userId the user ID
     * @return list of assigned tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getMyTasks(Long userId) {
        return taskRepository.findByAssigneeUserId(userId);
    }

    /**
     * Gets tasks created by a user.
     *
     * @param assignerId the assigner user ID
     * @return list of created tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getCreatedTasks(Long assignerId) {
        return taskRepository.findByAssignerId(assignerId);
    }

    /**
     * Gets tasks pending approval for a user.
     *
     * @param approverId the approver user ID
     * @return list of tasks pending approval
     */
    @Transactional(readOnly = true)
    public List<Task> getPendingApprovalTasks(Long approverId) {
        return taskRepository.findPendingApprovalByUserId(approverId);
    }

    /**
     * Gets tasks by department.
     *
     * @param departmentId the department ID
     * @return list of department tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getDepartmentTasks(Long departmentId) {
        return taskRepository.findByDepartmentId(departmentId);
    }

    // Private helper methods

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException("Task not found: " + taskId));
    }

    private void publishEvents(Task task) {
        task.getDomainEvents().forEach(eventPublisher::publish);
        task.clearDomainEvents();
    }
}
