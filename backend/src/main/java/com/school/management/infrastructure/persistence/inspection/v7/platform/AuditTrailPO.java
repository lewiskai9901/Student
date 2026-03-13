package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_audit_trail")
public class AuditTrailPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String userName;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String resourceName;
    private String details;
    private String ipAddress;
    private LocalDateTime occurredAt;

    @TableLogic
    private Integer deleted;
}
