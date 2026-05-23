package com.school.management.infrastructure.persistence.inspection.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("indicator_results")
public class IndicatorResultPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long orgUnitId;
    private Long indicatorId;
    private Long targetId;
    private String targetName;
    private String periodKey;
    private BigDecimal value;
    private Integer rankPosition;
    private String grade;
    private String status;
    private LocalDateTime computedAt;
    private LocalDateTime publishedAt;
    private Long revisionOf;
    /** JSON 数组 [submissionId, ...] */
    private String sourceSubmissionIds;
    /** JSON 数组 [sectionId, ...] */
    private String sourceSectionIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
