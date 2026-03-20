package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_violation_records")
public class ViolationRecordPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long submissionId;
    private Long submissionDetailId;
    private Long sectionId;
    private Long itemId;
    private Long userId;
    private String userName;
    private String classInfo;
    private LocalDateTime occurredAt;
    private String severity;
    private String description;
    private String evidenceUrls;
    private BigDecimal score;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
