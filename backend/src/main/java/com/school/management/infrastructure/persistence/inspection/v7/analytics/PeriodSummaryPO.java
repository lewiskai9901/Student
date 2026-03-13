package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insp_period_summaries")
public class PeriodSummaryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long projectId;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String targetType;
    private Long targetId;
    private String targetName;
    private Long orgUnitId;
    private String orgUnitName;
    private Integer inspectionDays;
    private BigDecimal avgScore;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private BigDecimal scoreStdDev;
    private String trendDirection;
    private BigDecimal trendPercent;
    private Integer ranking;
    private String dimensionScores;
    private String grade;
    private Integer correctiveCount;
    private Integer correctiveClosedCount;
    private BigDecimal prevPeriodScore;
    private BigDecimal momChange;
    private BigDecimal yoyScore;
    private BigDecimal yoyChange;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
