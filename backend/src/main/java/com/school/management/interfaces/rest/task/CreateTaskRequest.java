package com.school.management.interfaces.rest.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request for creating a task.
 */
@Data
public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    private String description;

    private Integer priority;

    private Long departmentId;

    private LocalDateTime dueDate;

    private Long workflowTemplateId;

    private List<Long> attachmentIds;

    private List<AssigneeInfo> assignees;

    @Data
    public static class AssigneeInfo {
        private Long userId;
        private String userName;
        private Long departmentId;
        private String departmentName;
    }
}
