package com.school.management.application.task.command;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建任务命令
 */
@Data
@Builder
public class CreateTaskCommand {

    private String title;
    private String description;
    private Integer priority;
    private Long assignerId;
    private String assignerName;
    private Integer assignType;
    private Long departmentId;
    private LocalDateTime dueDate;
    private List<Long> targetIds;
    private Long workflowTemplateId;
    private List<Long> attachmentIds;
}
