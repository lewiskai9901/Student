package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Persistence object for inspection records.
 */
@Data
@TableName("inspection_records")
public class InspectionRecordPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recordCode;

    private Long templateId;

    private Integer templateVersion;

    private Long roundId;

    private LocalDate inspectionDate;

    private String inspectionPeriod;

    private String status;

    private Long inspectorId;

    private String inspectorName;

    private LocalDateTime inspectedAt;

    private Long reviewerId;

    private LocalDateTime reviewedAt;

    private LocalDateTime publishedAt;

    private String remarks;

    private LocalDateTime createdAt;

    private Long createdBy;

    @TableLogic
    private Integer deleted;
}
