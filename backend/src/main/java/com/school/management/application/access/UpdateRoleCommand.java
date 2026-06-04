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
    /** 启用/禁用 (null=不改)。落库到 roles.status。 */
    private Boolean isEnabled;
}
