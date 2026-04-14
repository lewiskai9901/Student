package com.school.management.interfaces.rest.access;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating a new role.
 */
@Data
public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(max = 100, message = "Role name must not exceed 100 characters")
    private String roleName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private String roleType = "CUSTOM";
}
