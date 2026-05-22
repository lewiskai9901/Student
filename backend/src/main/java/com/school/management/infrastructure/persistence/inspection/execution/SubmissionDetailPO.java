package com.school.management.infrastructure.persistence.inspection.execution;

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
    @TableField(fill = FieldFill.INSERT)
    private Long orgUnitId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 申诉调整幂等标记: 非空表示该 detail 已应用过申诉调整, 防 AppealApprovedEvent 重复投递叠加扣分 */
    private LocalDateTime appealAdjustedAt;

    @TableLogic
    private Integer deleted;
}
