package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_template_module_refs")
public class TemplateModuleRefPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long compositeTemplateId;
    private Long moduleTemplateId;
    private Integer sortOrder;
    private Integer weight;
    private String overrideConfig;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
