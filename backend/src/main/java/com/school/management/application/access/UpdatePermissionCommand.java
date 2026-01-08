package com.school.management.application.access;

import lombok.Builder;
import lombok.Data;

/**
 * Command to update a permission.
 */
@Data
@Builder
public class UpdatePermissionCommand {
    private String permissionName;
    private String description;
}
