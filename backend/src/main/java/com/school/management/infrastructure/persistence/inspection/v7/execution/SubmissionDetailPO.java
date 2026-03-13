package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_submission_details")
public class SubmissionDetailPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long submissionId;
    private Long templateItemId;
    private String itemCode;
    private String itemName;
    private Long sectionId;
    private String sectionName;
    private String itemType;
    private String responseValue;
    private String scoringMode;
    private BigDecimal score;
    private String dimensions;
    private String scoringConfig;
    private String validationRules;
    private String conditionLogic;
    private BigDecimal itemWeight;
    private Integer timeSpentSeconds;
    private Boolean isFlagged;
    private String flagReason;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
