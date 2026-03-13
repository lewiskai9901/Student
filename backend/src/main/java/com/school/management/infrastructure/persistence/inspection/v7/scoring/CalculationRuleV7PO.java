package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insp_calculation_rules")
public class CalculationRuleV7PO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long scoringProfileId;
    private String ruleCode;
    private String ruleName;
    private Integer priority;
    private String ruleType;
    private String config;
    private Boolean isEnabled;
    private String scopeType;
    private String targetDimensionIds;
    private String activationCondition;
    private String appliesTo;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
    private String exclusionGroup;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
