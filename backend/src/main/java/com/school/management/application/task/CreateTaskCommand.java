package com.school.management.application.task;

import com.school.management.domain.task.model.TaskPriority;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Command for creating a new task.
 */
@Data
@Builder
public class CreateTaskCommand {
    private String title;
    private String description;
    private TaskPriority priority;
    private Long departmentId;
    private LocalDateTime dueDate;
    private Long workflowTemplateId;
    private List<Long> attachmentIds;
    private List<AssigneeInfo> assignees;
    private Long createdBy;
    private String createdByName;

    /**
     * Assignee information for task assignment.
     */
    @Data
    @Builder
    public static class AssigneeInfo {
        private Long userId;
        private String userName;
        private Long departmentId;
        private String departmentName;
    }
}
