package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insp_item_frequency_summaries")
public class ItemFrequencySummaryPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long projectId;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String itemCode;
    private String itemName;
    private Long sectionId;
    private String sectionName;
    private Integer occurrenceCount;
    private Integer flaggedCount;
    private BigDecimal totalDeduction;
    private BigDecimal avgDeduction;
    private BigDecimal cumulativePercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
