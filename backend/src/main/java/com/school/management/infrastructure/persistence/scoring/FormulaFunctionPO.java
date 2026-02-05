package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内置函数持久化对象
 */
@Data
@TableName("formula_functions")
public class FormulaFunctionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String category;

    /**
     * JSON格式存储
     */
    private String parametersDef;

    private String returnType;

    private String implementation;

    /**
     * JSON格式存储
     */
    private String examples;

    private Boolean isSystem;

    private Boolean isEnabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
