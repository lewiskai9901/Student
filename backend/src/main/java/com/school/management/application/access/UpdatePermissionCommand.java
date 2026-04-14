package com.school.management.application.access;

import com.school.management.domain.access.model.PermissionScope;
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
    private PermissionScope scope;
}
