package com.school.management.domain.access.model;

import com.school.management.domain.shared.Entity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * UserRole entity representing the assignment of a role to a user.
 */
@Getter
public class UserRole implements Entity<Long> {

    private Long id;
    private Long userId;
    private Long roleId;
    private Long orgUnitId;  // Optional: role scope limited to specific org unit
    private LocalDateTime assignedAt;
    private Long assignedBy;
    private LocalDateTime expiresAt;  // Optional: temporary role assignment
    private Boolean isActive;

    protected UserRole() {}

    @Builder
    public UserRole(Long id, Long userId, Long roleId, Long orgUnitId,
                    Long assignedBy, LocalDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.roleId = roleId;
        this.orgUnitId = orgUnitId;
        this.assignedAt = LocalDateTime.now();
        this.assignedBy = assignedBy;
        this.expiresAt = expiresAt;
        this.isActive = true;
    }

    /**
     * Assigns a role to a user.
     */
    public static UserRole assign(Long userId, Long roleId, Long assignedBy) {
        return UserRole.builder()
            .userId(userId)
            .roleId(roleId)
            .assignedBy(assignedBy)
            .build();
    }

    /**
     * Assigns a role to a user within a specific organization unit.
     */
    public static UserRole assignWithScope(Long userId, Long roleId, Long orgUnitId, Long assignedBy) {
        return UserRole.builder()
            .userId(userId)
            .roleId(roleId)
            .orgUnitId(orgUnitId)
            .assignedBy(assignedBy)
            .build();
    }

    /**
     * Assigns a temporary role to a user.
     */
    public static UserRole assignTemporary(Long userId, Long roleId, Long assignedBy, LocalDateTime expiresAt) {
        return UserRole.builder()
            .userId(userId)
            .roleId(roleId)
            .assignedBy(assignedBy)
            .expiresAt(expiresAt)
            .build();
    }

    /**
     * Checks if this role assignment is currently active.
     */
    public boolean isEffective() {
        if (!isActive) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    /**
     * Deactivates this role assignment.
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Reactivates this role assignment.
     */
    public void reactivate() {
        this.isActive = true;
    }

    /**
     * Extends the expiration of this role assignment.
     */
    public void extendExpiration(LocalDateTime newExpiresAt) {
        this.expiresAt = newExpiresAt;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
