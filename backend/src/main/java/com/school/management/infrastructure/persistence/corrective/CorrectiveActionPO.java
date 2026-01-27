package com.school.management.infrastructure.persistence.corrective;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("corrective_actions")
public class CorrectiveActionPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String actionCode;
    private String title;
    private String description;
    private String source;
    private Long sourceId;
    private String severity;
    private String category;
    private String status;
    private Long classId;
    private Long assigneeId;
    private LocalDateTime deadline;
    private String resolutionNote;

    /** JSON array of attachment URLs */
    private String resolutionAttachments;

    private LocalDateTime resolvedAt;
    private Long verifierId;
    private String verificationResult;
    private String verificationComment;
    private LocalDateTime verifiedAt;
    private Integer escalationLevel;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
