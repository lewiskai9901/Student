package com.school.management.infrastructure.persistence.tenant;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tenants")
public class TenantPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;
    private String tenantName;
    private String domain;
    private String config;
    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
