package com.school.management.interfaces.rest.access;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for assigning a role to a user with organization scope.
 */
@Data
public class AssignRoleWithScopeRequest {

    @NotNull(message = "Organization unit ID is required")
    private Long orgUnitId;
}
