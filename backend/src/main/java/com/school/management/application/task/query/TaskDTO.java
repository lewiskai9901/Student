package com.school.management.application.task.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务查询DTO
 */
@Data
@Builder
public class TaskDTO {

    private Long id;
    private String taskCode;
    private String title;
    private String description;
    private Integer priority;
    private String priorityName;
    private Long assignerId;
    private String assignerName;
    private Integer assignType;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime dueDate;
    private Integer status;
    private String statusName;
    private List<Long> targetIds;
    private LocalDateTime acceptedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private Long workflowTemplateId;
    private String processInstanceId;
    private String currentNode;
    private List<Long> currentApprovers;
    private List<Long> attachmentIds;
    private Boolean overdue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
