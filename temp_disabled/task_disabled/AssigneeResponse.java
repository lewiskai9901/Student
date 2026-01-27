package com.school.management.interfaces.rest.task;

import com.school.management.domain.task.model.AssigneeStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Assignee response DTO.
 */
@Data
public class AssigneeResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long departmentId;
    private String departmentName;
    private AssigneeStatus status;
    private LocalDateTime acceptedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
}
