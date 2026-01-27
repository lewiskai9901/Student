package com.school.management.interfaces.rest.access;

import com.school.management.domain.access.model.DataScope;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for updating an existing role.
 */
@Data
public class UpdateRoleRequest {

    @Size(max = 100, message = "Role name must not exceed 100 characters")
    private String roleName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private DataScope dataScope;
}
