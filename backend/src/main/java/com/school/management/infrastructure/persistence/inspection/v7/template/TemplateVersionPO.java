package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_template_versions")
public class TemplateVersionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long templateId;
    private Integer version;
    private String structureSnapshot;
    private String scoringProfileSnapshot;
    private Long createdBy;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
