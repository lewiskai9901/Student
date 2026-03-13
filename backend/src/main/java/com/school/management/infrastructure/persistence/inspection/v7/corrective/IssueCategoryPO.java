package com.school.management.infrastructure.persistence.inspection.v7.corrective;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_issue_categories")
public class IssueCategoryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long parentId;
    private String categoryCode;
    private String categoryName;
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
