package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistence object for appeals.
 */
@Data
@TableName("appeals")
public class AppealPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long inspectionRecordId;

    private Long deductionDetailId;

    private Long classId;

    private String appealCode;

    private String reason;

    /**
     * JSON array of attachment URLs.
     */
    private String attachments;

    private BigDecimal originalDeduction;

    private BigDecimal requestedDeduction;

    private BigDecimal approvedDeduction;

    private String status;

    private Long applicantId;

    private LocalDateTime appliedAt;

    private Long level1ReviewerId;

    private LocalDateTime level1ReviewedAt;

    private String level1Comment;

    private Long level2ReviewerId;

    private LocalDateTime level2ReviewedAt;

    private String level2Comment;

    private LocalDateTime effectiveAt;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
