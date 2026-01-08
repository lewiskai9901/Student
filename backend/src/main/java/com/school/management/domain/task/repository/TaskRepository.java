package com.school.management.domain.task.repository;

import com.school.management.domain.task.model.Task;
import com.school.management.domain.task.model.TaskStatus;

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
}
