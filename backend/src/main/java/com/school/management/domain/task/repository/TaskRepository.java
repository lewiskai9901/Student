package com.school.management.domain.task.repository;

import com.school.management.domain.task.model.aggregate.Task;
import com.school.management.domain.task.model.valueobject.TaskPriority;
import com.school.management.domain.task.model.valueobject.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Task aggregate.
 *
 * <p>This interface defines the contract for persisting and retrieving
 * Task aggregates. Implementations should handle:
 * <ul>
 *   <li>Task persistence with all child entities (Assignees, Submissions)</li>
 *   <li>Optimistic locking for concurrent access</li>
 *   <li>Domain event publishing after save</li>
 * </ul>
 */
public interface TaskRepository {

    /**
     * Saves a task aggregate.
     *
     * @param task the task to save
     * @return the saved task with generated IDs
     */
    Task save(Task task);

    /**
     * Finds a task by its ID.
     *
     * @param id the task ID
     * @return the task or empty if not found
     */
    Optional<Task> findById(Long id);

    /**
     * Finds a task by its task code.
     *
     * @param taskCode the unique task code
     * @return the task or empty if not found
     */
    Optional<Task> findByTaskCode(String taskCode);

    /**
     * Finds tasks assigned to a specific user.
     *
     * @param userId the assignee user ID
     * @return list of tasks assigned to the user
     */
    List<Task> findByAssigneeUserId(Long userId);

    /**
     * Finds tasks created by a specific user.
     *
     * @param assignerId the assigner user ID
     * @return list of tasks created by the user
     */
    List<Task> findByAssignerId(Long assignerId);

    /**
     * Finds tasks by status.
     *
     * @param status the task status
     * @return list of tasks with the specified status
     */
    List<Task> findByStatus(TaskStatus status);

    /**
     * Finds tasks by department.
     *
     * @param departmentId the department ID
     * @return list of tasks in the department
     */
    List<Task> findByDepartmentId(Long departmentId);

    /**
     * Finds overdue tasks.
     *
     * @param now current time
     * @return list of overdue tasks that are not completed
     */
    List<Task> findOverdueTasks(LocalDateTime now);

    /**
     * Finds tasks pending approval for a user.
     *
     * @param approverId the approver user ID
     * @return list of tasks pending the user's approval
     */
    List<Task> findPendingApprovalByUserId(Long approverId);

    /**
     * Finds tasks pending approval for a user (alias).
     *
     * @param approverId the approver user ID
     * @return list of tasks pending the user's approval
     */
    default List<Task> findPendingApprovalByApproverId(Long approverId) {
        return findPendingApprovalByUserId(approverId);
    }

    /**
     * Finds pending tasks for an assignee.
     *
     * @param assigneeId the assignee user ID
     * @return list of pending tasks
     */
    List<Task> findPendingByAssigneeId(Long assigneeId);

    /**
     * Finds in-progress tasks for an assignee.
     *
     * @param assigneeId the assignee user ID
     * @return list of in-progress tasks
     */
    List<Task> findInProgressByAssigneeId(Long assigneeId);

    /**
     * Finds overdue tasks (alias for findOverdueTasks).
     *
     * @param now current time
     * @return list of overdue tasks
     */
    default List<Task> findOverdue(LocalDateTime now) {
        return findOverdueTasks(now);
    }

    /**
     * Counts tasks by department.
     *
     * @param departmentId the department ID
     * @return count of tasks in the department
     */
    long countByDepartmentId(Long departmentId);

    /**
     * Checks if a task exists with the given task code.
     *
     * @param taskCode the task code
     * @return true if exists
     */
    boolean existsByTaskCode(String taskCode);

    /**
     * Counts tasks by status for a user.
     *
     * @param userId the user ID (as assignee)
     * @param status the task status
     * @return count of matching tasks
     */
    long countByAssigneeUserIdAndStatus(Long userId, TaskStatus status);

    /**
     * Counts all tasks by status.
     *
     * @param status the task status
     * @return count of tasks with the status
     */
    long countByStatus(TaskStatus status);

    /**
     * Deletes a task by ID.
     *
     * @param id the task ID
     */
    void deleteById(Long id);

    /**
     * Checks if a task exists with the given ID.
     *
     * @param id the task ID
     * @return true if exists
     */
    boolean existsById(Long id);

    /**
     * Gets the next value for task ID generation.
     *
     * @return the next ID value
     */
    Long nextId();

    /**
     * Finds tasks by criteria with pagination.
     *
     * @param criteria the query criteria
     * @param pageNum the page number (1-based)
     * @param pageSize the page size
     * @return list of matching tasks
     */
    List<Task> findByPage(TaskQueryCriteria criteria, int pageNum, int pageSize);

    /**
     * Counts tasks matching the criteria.
     *
     * @param criteria the query criteria
     * @return count of matching tasks
     */
    long countByCriteria(TaskQueryCriteria criteria);

    /**
     * Query criteria for task searches.
     */
    @Data
    class TaskQueryCriteria {
        private String keyword;
        private Long assignerId;
        private Long assigneeId;
        private Long departmentId;
        private TaskStatus status;
        private TaskPriority priority;
        private LocalDateTime dueDateFrom;
        private LocalDateTime dueDateTo;
        private Boolean overdue;
    }
}
