package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Persistence object for inspection sessions.
 */
@Data
@TableName("inspection_sessions")
public class InspectionSessionPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String sessionCode;

    private Long templateId;

    private Integer templateVersion;

    private LocalDate inspectionDate;

    private String inspectionPeriod;

    private String inputMode;

    private String scoringMode;

    private Integer baseScore;

    private String status;

    private Long inspectorId;

    private String inspectorName;

    private LocalDateTime submittedAt;

    private LocalDateTime publishedAt;

    private String remarks;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
