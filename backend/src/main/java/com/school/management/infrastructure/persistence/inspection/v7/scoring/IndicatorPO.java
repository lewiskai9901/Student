package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_indicators")
public class IndicatorPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long projectId;
    private Long parentIndicatorId;
    private String name;
    private String indicatorType;
    private Long sourceSectionId;
    private String sourceAggregation;
    private String compositeAggregation;
    private String missingPolicy;
    private String normalization;
    private String normalizationConfig;
    private String evaluationPeriod;
    private Long gradeSchemeId;
    private String evaluationMethod;
    private String gradeThresholds;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
