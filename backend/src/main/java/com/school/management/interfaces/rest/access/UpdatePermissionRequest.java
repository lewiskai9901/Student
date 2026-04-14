package com.school.management.interfaces.rest.access;

import com.school.management.domain.access.model.PermissionScope;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for updating an existing permission.
 */
@Data
public class UpdatePermissionRequest {

    @Size(max = 100, message = "Permission name must not exceed 100 characters")
    private String permissionName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Null means "don't change"; non-null replaces the current scope.
     */
    private PermissionScope scope;
}
