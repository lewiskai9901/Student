package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_project_inspectors")
public class ProjectInspectorPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    @TableField(fill = FieldFill.INSERT)
    private Long orgUnitId;               // 数据权限边界 (从 project 继承, MetaObjectHandler 自动填充)
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
