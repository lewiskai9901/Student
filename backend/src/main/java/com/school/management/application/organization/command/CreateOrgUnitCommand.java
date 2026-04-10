package com.school.management.application.organization.command;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Command to create a new organization unit.
 */
@Data
@Builder
public class CreateOrgUnitCommand {

    private String unitCode;
    private String unitName;
    private String unitType;  // typeCode from org_unit_types
    private Long parentId;
    private Long createdBy;

    /** 扩展属性（来自 DynamicForm，存入 org_units.attributes） */
    private Map<String, Object> attributes;
}
