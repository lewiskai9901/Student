package com.school.management.application.scoring.dto;

import lombok.Data;

/**
 * 内置变量DTO
 */
@Data
public class FormulaVariableDTO {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String valueType;
    private String defaultValue;
    private String sourceDescription;
    private Boolean isSystem;
    private Boolean isEnabled;
}
