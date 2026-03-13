package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_report_templates")
public class ReportTemplatePO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String templateName;
    private String templateCode;
    private String reportType;
    private String formatConfig;
    private String headerConfig;
    private Boolean isDefault;
    private Boolean isEnabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
