package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_library_items")
public class LibraryItemPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String itemCode;
    private String itemName;
    private String description;
    private String itemType;
    private String category;
    private String tags;
    private String defaultConfig;
    private String defaultValidationRules;
    private String defaultScoringConfig;
    private String defaultHelpContent;
    private Integer usageCount;
    private Boolean isStandard;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
