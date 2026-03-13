package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_task_section_assignments")
public class TaskSectionAssignmentPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long sectionId;
    private Long inspectorId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
