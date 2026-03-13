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
    private Long parentProjectId;
    private String projectCode;
    private String projectName;
    private Long templateId;
    private Long templateVersionId;
    private Long scoringProfileId;
    private String scopeType;
    private String scopeConfig;
    private String targetType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String cycleType;
    private String cycleConfig;
    private String timeSlots;
    private Boolean skipHolidays;
    private Long holidayCalendarId;
    private String excludedDates;
    private String assignmentMode;
    private Boolean reviewRequired;
    private Boolean autoPublish;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
