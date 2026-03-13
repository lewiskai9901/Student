package com.school.management.infrastructure.persistence.inspection.v7.corrective;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_corrective_cases")
public class CorrectiveCasePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String caseCode;
    private Long submissionId;
    private Long detailId;
    private Long projectId;
    private Long taskId;
    private String targetType;
    private Long targetId;
    private String targetName;
    private String issueDescription;
    private String requiredAction;
    private Long issueCategoryId;
    private String deficiencyCode;
    private String rcaMethod;
    private String rcaData;
    private String preventiveAction;
    private String priority;
    private LocalDateTime deadline;
    private Long assigneeId;
    private String assigneeName;
    private Integer escalationLevel;
    private String status;
    private String correctionNote;
    private String correctionEvidenceIds;
    private LocalDateTime correctedAt;
    private Long verifierId;
    private String verifierName;
    private LocalDateTime verifiedAt;
    private String verificationNote;
    private java.time.LocalDate effectivenessCheckDate;
    private String effectivenessStatus;
    private String effectivenessNote;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
