package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insp_corrective_summaries")
public class CorrectiveSummaryPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long projectId;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer totalCases;
    private Integer openCases;
    private Integer inProgressCases;
    private Integer closedCases;
    private Integer overdueCases;
    private Integer escalatedCases;
    private BigDecimal avgResolutionDays;
    private BigDecimal onTimeRate;
    private Integer effectivenessConfirmed;
    private Integer effectivenessFailed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
