package com.school.management.infrastructure.persistence.inspection.appeal;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inspection_appeals")
public class InspAppealPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    @TableField(fill = FieldFill.INSERT)
    private Long orgUnitId;
    private String appealCode;
    private Long submissionDetailId;
    private Long submissionId;
    private Long taskId;
    private Long projectId;
    private String subjectType;
    private Long subjectId;
    private Long submitterUserId;
    private String submitterName;
    private String reason;
    private String attachments;
    private BigDecimal expectedAdjustment;
    private BigDecimal finalAdjustment;
    private String status;
    private Long reviewerId;
    private String reviewerName;
    private String reviewerComment;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
