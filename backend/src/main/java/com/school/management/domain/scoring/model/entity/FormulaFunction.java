package com.school.management.domain.scoring.model.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 内置函数实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormulaFunction {

    /**
     * ID
     */
    private Long id;

    /**
     * 函数名
     */
    private String name;

    /**
     * 函数描述
     */
    private String description;

    /**
     * 分类：math/array/logic/date/string/lookup
     */
    private String category;

    /**
     * 参数定义
     */
    private List<Map<String, Object>> parametersDef;

    /**
     * 返回类型
     */
    private String returnType;

    /**
     * JavaScript实现代码
     */
    private String implementation;

    /**
     * 使用示例
     */
    private List<Map<String, Object>> examples;

    /**
     * 是否系统内置
     */
    private Boolean isSystem;

    /**
     * 是否启用
     */
    private Boolean isEnabled;
}
