package com.school.management.application.organization.command;

import com.school.management.domain.organization.model.OrgUnitType;
import lombok.Builder;
import lombok.Data;

/**
 * Command to create a new organization unit.
 */
@Data
@Builder
public class CreateOrgUnitCommand {

    private String unitCode;
    private String unitName;
    private OrgUnitType unitType;
    private Long parentId;
    private Long createdBy;
}
