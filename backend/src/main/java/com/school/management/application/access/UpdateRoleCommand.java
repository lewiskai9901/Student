package com.school.management.application.access;

import lombok.Builder;
import lombok.Data;

/**
 * Command to update a role.
 */
@Data
@Builder
public class UpdateRoleCommand {
    private String roleName;
    private String description;
}
