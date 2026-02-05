package com.school.management.domain.scoring.model.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 内置变量实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormulaVariable {

    /**
     * ID
     */
    private Long id;

    /**
     * 变量名
     */
    private String name;

    /**
     * 变量描述
     */
    private String description;

    /**
     * 分类：score/count/time/context/input
     */
    private String category;

    /**
     * 值类型：number/string/boolean/array/object/any
     */
    private String valueType;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 值来源说明
     */
    private String sourceDescription;

    /**
     * 是否系统内置
     */
    private Boolean isSystem;

    /**
     * 是否启用
     */
    private Boolean isEnabled;
}
