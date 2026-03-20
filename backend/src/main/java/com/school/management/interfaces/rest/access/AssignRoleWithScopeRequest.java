package com.school.management.interfaces.rest.access;

import lombok.Data;

/**
 * Request DTO for assigning a role to a user with scope.
 */
@Data
public class AssignRoleWithScopeRequest {

    /** Scope type: ALL or ORG_UNIT. Defaults to ALL if not provided. */
    private String scopeType;

    /** Scope ID: 0 for ALL, orgUnitId for ORG_UNIT. */
    private Long scopeId;
}
