package com.school.management.infrastructure.persistence.inspection.v7.corrective;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insp_corrective_subtasks")
public class CorrectiveSubtaskPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long caseId;
    private String subtaskName;
    private String description;
    private Long assigneeId;
    private String status;
    private Integer priority;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
