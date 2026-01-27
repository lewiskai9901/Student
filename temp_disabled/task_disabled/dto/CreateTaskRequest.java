package com.school.management.interfaces.rest.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建任务请求
 */
@Data
public class CreateTaskRequest {

    @NotBlank(message = "任务标题不能为空")
    private String title;

    private String description;

    private Integer priority;

    private Long departmentId;

    @NotNull(message = "截止时间不能为空")
    private LocalDateTime dueDate;

    @NotNull(message = "执行人不能为空")
    private List<Long> targetIds;

    private Integer assignType;

    private Long workflowTemplateId;

    private List<Long> attachmentIds;
}
