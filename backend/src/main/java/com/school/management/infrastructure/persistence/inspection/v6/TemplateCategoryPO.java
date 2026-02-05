package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模板类别持久化对象
 */
@Data
@TableName("template_categories")
public class TemplateCategoryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private String categoryCode;

    private String categoryName;

    private String description;

    private String icon;

    private String color;

    private BigDecimal weight;

    private BigDecimal maxScore;

    private Integer sortOrder;

    private Boolean isEnabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
