package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_submissions")
public class InspSubmissionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long taskId;
    private Long sectionId;
    private String targetType;
    private Long targetId;
    private String targetName;
    private Long rootTargetId;
    private String rootTargetName;
    private Long orgUnitId;
    private String orgUnitName;
    private BigDecimal weightRatio;
    private String status;
    private String formData;
    private String scoreBreakdown;
    private BigDecimal baseScore;
    private BigDecimal finalScore;
    private BigDecimal deductionTotal;
    private BigDecimal bonusTotal;
    private String grade;
    private Boolean passed;
    private Integer totalTimeSeconds;
    private String nfcTagUid;
    private LocalDateTime checkinTime;
    private Integer syncVersion;
    private LocalDateTime completedAt;
    private LocalDateTime closedAt;
    private String closedReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
