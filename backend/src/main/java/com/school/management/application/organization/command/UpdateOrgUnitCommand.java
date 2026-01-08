package com.school.management.application.organization.command;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Command to update an organization unit.
 */
@Data
@Builder
public class UpdateOrgUnitCommand {

    private String unitName;
    private Long leaderId;
    private List<Long> deputyLeaderIds;
    private Integer sortOrder;
    private Long updatedBy;
}
