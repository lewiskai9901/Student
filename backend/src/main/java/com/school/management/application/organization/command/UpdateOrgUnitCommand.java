package com.school.management.application.organization.command;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Command to update an organization unit.
 */
@Data
@Builder
public class UpdateOrgUnitCommand {

    private String unitName;
    private Integer sortOrder;
    private Integer headcount;
    private Map<String, Object> attributes;
    private String reason;
    private Long updatedBy;
}
