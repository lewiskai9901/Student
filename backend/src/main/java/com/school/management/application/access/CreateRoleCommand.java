package com.school.management.application.access;

import lombok.Builder;
import lombok.Data;

/**
 * Command to create a new role.
 */
@Data
@Builder
public class CreateRoleCommand {
    private String roleCode;
    private String roleName;
    private String description;
    private String roleType;
    private Long createdBy;
}
