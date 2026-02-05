package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 打分方式持久化对象
 */
@Data
@TableName("input_types")
public class InputTypePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String description;

    private String category;

    private String componentType;

    /**
     * JSON格式存储
     */
    private String componentConfig;

    private String valueType;

    private String valueMapping;

    private String validationRules;

    private Boolean isSystem;

    private Boolean isEnabled;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
