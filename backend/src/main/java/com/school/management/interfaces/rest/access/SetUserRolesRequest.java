package com.school.management.interfaces.rest.access;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for setting user roles with scope support.
 */
@Data
public class SetUserRolesRequest {

    @NotNull(message = "Role assignments are required")
    private List<RoleAssignmentItem> assignments;

    @Data
    public static class RoleAssignmentItem {
        @NotNull(message = "Role ID is required")
        private Long roleId;

        /** Scope type: ALL or ORG_UNIT. Defaults to ALL. */
        private String scopeType;

        /** Scope ID: 0 for ALL, orgUnitId for ORG_UNIT. */
        private Long scopeId;

        /** Expiry time (null = permanent). */
        private String expiresAt;

        /** Authorization reason. */
        private String reason;
    }
}
