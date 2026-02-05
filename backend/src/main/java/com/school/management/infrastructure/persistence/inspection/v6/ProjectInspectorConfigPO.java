package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * V6项目检查员配置持久化对象
 */
@Data
@TableName("project_inspector_configs")
public class ProjectInspectorConfigPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long inspectorId;
    private String inspectorName;

    private Boolean isDefault;
    private String scopeType;
    private String scopeIds;

    private String availableDays;
    private String availableTimeSlots;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
