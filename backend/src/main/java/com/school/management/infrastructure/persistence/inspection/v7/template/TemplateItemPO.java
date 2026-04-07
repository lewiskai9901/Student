package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_template_items")
public class TemplateItemPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long sectionId;
    private String itemCode;
    private String itemName;
    private String description;
    private String itemType;
    private String config;
    private String validationRules;
    private Long responseSetId;
    private String scoringConfig;
    private Long dimensionId;
    private String helpContent;
    private Boolean isRequired;
    private Boolean isScored;
    private Boolean requireEvidence;
    private BigDecimal itemWeight;
    private Integer sortOrder;
    private String conditionLogic;
    private Long libraryItemId;
    private Boolean syncWithLibrary;
    private String visibilityLogic;
    private String scoringLogic;
    private String inputMode;
    private String linkedEventTypeCode;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
