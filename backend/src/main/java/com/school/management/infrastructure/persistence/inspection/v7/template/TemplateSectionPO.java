package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_template_sections")
public class TemplateSectionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long templateId;
    private String sectionCode;
    private String sectionName;
    private Integer sortOrder;
    private Integer weight;
    private Boolean isRepeatable;
    private String conditionLogic;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
