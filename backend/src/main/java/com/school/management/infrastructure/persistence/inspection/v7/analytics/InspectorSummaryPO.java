package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insp_inspector_summaries")
public class InspectorSummaryPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long projectId;
    private Long inspectorId;
    private String inspectorName;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer cancelledTasks;
    private Integer expiredTasks;
    private BigDecimal avgCompletionTimeMinutes;
    private BigDecimal avgScore;
    private Integer totalSubmissions;
    private Integer flaggedSubmissions;
    private BigDecimal complianceRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
