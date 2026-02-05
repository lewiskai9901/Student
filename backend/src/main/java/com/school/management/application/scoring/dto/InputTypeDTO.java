package com.school.management.application.scoring.dto;

import lombok.Data;
import java.util.Map;

/**
 * 打分方式DTO
 */
@Data
public class InputTypeDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String category;
    private String componentType;
    private String componentTypeName;
    private Map<String, Object> componentConfig;
    private String valueType;
    private Map<String, Object> valueMapping;
    private Map<String, Object> validationRules;
    private Boolean isSystem;
    private Boolean isEnabled;
    private Integer sortOrder;
}
