package com.school.management.infrastructure.persistence.task;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务持久化对象
 */
@Data
@TableName(value = "tasks", autoResultMap = true)
public class TaskPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskCode;

    private String title;

    private String description;

    private Integer priority;

    private Long assignerId;

    private String assignerName;

    private Integer assignType;

    private Long departmentId;

    private LocalDateTime dueDate;

    private Integer status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> targetIds;

    private LocalDateTime acceptedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime completedAt;

    private Long workflowTemplateId;

    private String processInstanceId;

    private String currentNode;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> currentApprovers;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> attachmentIds;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
