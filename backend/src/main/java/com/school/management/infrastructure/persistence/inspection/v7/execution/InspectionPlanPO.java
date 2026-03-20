package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_inspection_plans")
public class InspectionPlanPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long projectId;
    private String planName;
    private String sectionIds;
    private String scheduleMode;
    private String cycleType;
    private Integer frequency;
    private String scheduleDays;
    private String timeSlots;
    private Boolean skipHolidays;
    private Boolean isEnabled;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
