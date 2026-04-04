package com.school.management.interfaces.rest.access;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for user role assignment information.
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class UserRoleResponse {

    private Long id;

    private Long userId;

    private Long roleId;

    private String roleName;

    private String roleCode;

    private String scopeType;

    private Long scopeId;

    private String scopeName;

    private LocalDateTime assignedAt;

    private LocalDateTime expiresAt;

    private Boolean isActive;

    private String reason;

    private Long grantedBy;

    private LocalDateTime grantedAt;
}
