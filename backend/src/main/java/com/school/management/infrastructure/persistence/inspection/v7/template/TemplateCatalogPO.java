package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_template_catalogs")
public class TemplateCatalogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long parentId;
    private String catalogCode;
    private String catalogName;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Boolean isEnabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
