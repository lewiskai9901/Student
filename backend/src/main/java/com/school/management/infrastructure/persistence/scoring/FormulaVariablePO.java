package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内置变量持久化对象
 */
@Data
@TableName("formula_variables")
public class FormulaVariablePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String category;

    private String valueType;

    private String defaultValue;

    private String sourceDescription;

    private Boolean isSystem;

    private Boolean isEnabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
