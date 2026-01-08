package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for inspection categories.
 */
@Data
@TableName("inspection_categories")
public class InspectionCategoryPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long templateId;

    private String categoryCode;

    private String categoryName;

    private String description;

    private Integer baseScore;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
