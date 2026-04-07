package com.school.management.application.organization.command;

import lombok.Builder;
import lombok.Data;

import java.util.List;
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

    /**
     * User-selected positions to create.
     * Each entry has positionName + headcount.
     * If null, no positions are auto-created (template is just a menu).
     */
    private List<SelectedPosition> selectedPositions;

    /** 扩展属性（来自 DynamicForm，存入 org_units.attributes） */
    private Map<String, Object> attributes;

    @Data
    public static class SelectedPosition {
        private String positionName;
        private int headcount;
    }
}
