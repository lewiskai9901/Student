package com.school.management.infrastructure.persistence.schedule;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for schedule policies.
 */
@Data
@TableName("schedule_policies")
public class SchedulePolicyPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String policyCode;

    private String policyName;

    private String policyType;

    private String rotationAlgorithm;

    private Long templateId;

    private String inspectorPool;       // JSON string

    private String scheduleConfig;      // JSON string

    private String excludedDates;       // JSON string

    private Boolean isEnabled;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
