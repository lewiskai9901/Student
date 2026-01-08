package com.school.management.interfaces.rest.access;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

/**
 * Request DTO for setting user roles.
 */
@Data
public class SetUserRolesRequest {

    @NotNull(message = "Role IDs are required")
    private Set<Long> roleIds;
}
