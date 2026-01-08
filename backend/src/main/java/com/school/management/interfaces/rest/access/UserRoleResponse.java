package com.school.management.interfaces.rest.access;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for user role assignment information.
 */
@Data
public class UserRoleResponse {

    private Long id;

    private Long userId;

    private Long roleId;

    private Long orgUnitId;

    private LocalDateTime assignedAt;

    private LocalDateTime expiresAt;

    private Boolean isActive;
}
