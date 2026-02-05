package com.school.management.application.scoring.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 内置函数DTO
 */
@Data
public class FormulaFunctionDTO {
    private Long id;
    private String name;
    private String description;
    private String category;
    private List<Map<String, Object>> parametersDef;
    private String returnType;
    private String implementation;
    private List<Map<String, Object>> examples;
    private Boolean isSystem;
    private Boolean isEnabled;
}
