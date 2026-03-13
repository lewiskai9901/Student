package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_templates")
public class InspTemplatePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String templateCode;
    private String templateName;
    private String description;
    private Long catalogId;
    private String tags;
    private String targetType;
    private Integer latestVersion;
    private String status;
    private Boolean isDefault;
    private Integer useCount;
    private LocalDateTime lastUsedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
