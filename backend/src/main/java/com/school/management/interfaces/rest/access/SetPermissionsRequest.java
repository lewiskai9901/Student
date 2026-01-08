package com.school.management.interfaces.rest.access;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

/**
 * Request DTO for setting role permissions.
 */
@Data
public class SetPermissionsRequest {

    @NotNull(message = "Permission IDs are required")
    private Set<Long> permissionIds;
}
