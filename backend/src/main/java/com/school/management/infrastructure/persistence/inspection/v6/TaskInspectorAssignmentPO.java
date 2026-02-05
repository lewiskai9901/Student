package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * V6任务检查员分配持久化对象
 */
@Data
@TableName("task_inspector_assignments")
public class TaskInspectorAssignmentPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long inspectorId;
    private String inspectorName;

    private String scopeType;
    private String scopeIds;

    private String status;
    private LocalDateTime acceptedAt;

    private Long createdBy;
    private LocalDateTime createdAt;
}
