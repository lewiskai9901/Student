package com.school.management.application.scoring.dto;

import lombok.Data;
import java.util.Map;

/**
 * 创建打分方式命令
 */
@Data
public class CreateInputTypeCommand {
    private String code;
    private String name;
    private String description;
    private String category;
    private String componentType;
    private Map<String, Object> componentConfig;
    private String valueType;
    private Map<String, Object> valueMapping;
    private Map<String, Object> validationRules;
}
