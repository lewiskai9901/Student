package com.school.management.interfaces.rest.task;

import com.school.management.domain.task.model.Task;
import com.school.management.domain.task.model.TaskPriority;
import com.school.management.domain.task.model.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Task response DTO.
 */
@Data
public class TaskResponse {
    private Long id;
    private String taskCode;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private Long assignerId;
    private String assignerName;
    private Task.AssignmentType assignmentType;
    private Long departmentId;
    private LocalDateTime dueDate;
    private LocalDateTime acceptedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private int assigneeCount;
    private int completedAssigneeCount;
    private int completionPercentage;
    private boolean overdue;
    private LocalDateTime createdAt;
    private List<String> allowedTransitions;
    private List<AssigneeResponse> assignees;
}
