package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insp_projects")
public class InspProjectPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String projectCode;
    private String projectName;
    private Long templateId;          // maps to rootSectionId in domain
    private Long templateVersionId;
    private Long scoringProfileId;
    private String scopeType;
    private String scopeConfig;
    private LocalDate startDate;
    private LocalDate endDate;
    private String assignmentMode;
    private Boolean reviewRequired;
    private Boolean autoPublish;
    private String evaluationMode;
    private String multiRaterMode;
    private String raterWeightBy;
    private java.math.BigDecimal consensusThreshold;
    private Boolean trendEnabled;
    private Integer trendLookbackDays;
    private Boolean decayEnabled;
    private String decayMode;
    private Boolean calibrationEnabled;
    private String calibrationMethod;
    private String splitStrategy;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
