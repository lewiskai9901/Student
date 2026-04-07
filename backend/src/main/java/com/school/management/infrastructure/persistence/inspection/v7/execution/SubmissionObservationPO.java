package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_submission_observations")
public class SubmissionObservationPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;

    private Long submissionId;
    private Long detailId;
    private Long projectId;
    private Long taskId;

    private String itemCode;
    private String itemName;
    private String itemType;
    private String sectionName;

    private String subjectType;
    private Long subjectId;
    private String subjectName;
    private Long classId;
    private String className;

    private BigDecimal score;
    private Integer isNegative;
    private String severity;
    private Integer isFlagged;

    private String linkedEventTypeCode;

    private String responseValue;
    private String description;

    private LocalDateTime observedAt;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
