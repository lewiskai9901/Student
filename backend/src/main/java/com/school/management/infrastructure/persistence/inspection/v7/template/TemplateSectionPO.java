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
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long parentSectionId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long refTemplateId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String scoringConfig;
    private String sectionCode;
    private String sectionName;
    private String targetType;
    private String targetSourceMode;
    private String targetTypeFilter;
    private String description;
    private String tags;
    private Long catalogId;
    private String status;
    private Integer latestVersion;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long refSectionId;
    private Integer sortOrder;
    private Boolean isRepeatable;
    private String conditionLogic;
    private String inputMode;
    private String inspectionMode;
    private String continuousStart;
    private String continuousEnd;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
