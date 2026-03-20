package com.school.management.application.organization.command;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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

    @Data
    public static class SelectedPosition {
        private String positionName;
        private int headcount;
    }
}
