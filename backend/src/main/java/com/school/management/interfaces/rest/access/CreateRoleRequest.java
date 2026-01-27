package com.school.management.interfaces.rest.access;

import com.school.management.casbin.model.DataScope;
import com.school.management.domain.access.model.RoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating a new role.
 */
@Data
public class CreateRoleRequest {

    @NotBlank(message = "Role code is required")
    @Size(max = 50, message = "Role code must not exceed 50 characters")
    private String roleCode;

    @NotBlank(message = "Role name is required")
    @Size(max = 100, message = "Role name must not exceed 100 characters")
    private String roleName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Role type is required")
    private RoleType roleType;

    private DataScope dataScope = DataScope.SELF;
}
