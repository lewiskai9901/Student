package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspection_templates")
public class InspectionTemplatePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateCode;
    private String templateName;
    private String description;
    private Integer currentVersion;
    private String applicableScope;
    private String status;
    private Boolean isDefault;
    private Integer useCount;
    private LocalDateTime lastUsedAt;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
