package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("insp_tasks")
public class InspTaskPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String taskCode;
    private Long projectId;
    private LocalDate taskDate;
    private String timeSlotCode;
    private LocalTime timeSlotStart;
    private LocalTime timeSlotEnd;
    private Long inspectorId;
    private String inspectorName;
    private Long reviewerId;
    private String reviewerName;
    private String status;
    private Integer totalTargets;
    private Integer completedTargets;
    private Integer skippedTargets;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime publishedAt;
    private String reviewComment;
    private String collaborationMode;
    private LocalDateTime executionStartedAt;
    private LocalDateTime executionEndedAt;
    private String assignedSectionIds;    // JSON: assigned section IDs
    private String assignedTargetIds;     // JSON: assigned target IDs
    private Long inspectionPlanId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
