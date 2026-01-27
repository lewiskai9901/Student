package com.school.management.infrastructure.persistence.schedule;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Persistence object for schedule executions.
 */
@Data
@TableName("schedule_executions")
public class ScheduleExecutionPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long policyId;

    private LocalDate executionDate;

    private String assignedInspectors;  // JSON string

    private Long sessionId;

    private String status;

    private String failureReason;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
