package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for appeal approval records.
 */
@Data
@TableName("appeal_approvals")
public class AppealApprovalPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long appealId;

    private Long reviewerId;

    private String reviewLevel;

    private String action;

    private String comment;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;
}
