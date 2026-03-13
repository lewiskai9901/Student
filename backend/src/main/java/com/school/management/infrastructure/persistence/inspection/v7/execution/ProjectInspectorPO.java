package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_project_inspectors")
public class ProjectInspectorPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long projectId;
    private Long userId;
    private String userName;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
