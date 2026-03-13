package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insp_daily_summaries")
public class DailySummaryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long projectId;
    private LocalDate summaryDate;
    private String targetType;
    private Long targetId;
    private String targetName;
    private Long orgUnitId;
    private String orgUnitName;
    private Integer inspectionCount;
    private BigDecimal avgScore;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private BigDecimal totalDeductions;
    private BigDecimal totalBonuses;
    private Integer passCount;
    private Integer failCount;
    private Integer ranking;
    private String dimensionScores;
    private String grade;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
