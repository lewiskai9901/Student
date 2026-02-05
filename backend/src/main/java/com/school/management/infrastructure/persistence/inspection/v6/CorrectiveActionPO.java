package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V6整改记录持久化对象
 */
@Data
@TableName("v6_corrective_actions")
public class CorrectiveActionPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long detailId;
    private Long targetId;
    private Long taskId;
    private Long projectId;
    private String actionCode;
    private String issueDescription;
    private String requiredAction;
    private LocalDate deadline;
    private Long assigneeId;
    private String assigneeName;
    private String status;
    private String correctionNote;
    private String evidenceIds;
    private LocalDateTime correctedAt;
    private Long verifierId;
    private String verifierName;
    private LocalDateTime verifiedAt;
    private String verificationNote;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
