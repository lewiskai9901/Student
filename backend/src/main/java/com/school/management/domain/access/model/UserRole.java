package com.school.management.domain.access.model;

import com.school.management.domain.shared.Entity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * UserRole entity representing the assignment of a role to a user with scope.
 *
 * Scope determines the data boundary for this role assignment:
 * - ALL (scopeId=0): Global scope, no data restriction from this assignment
 * - ORG_UNIT (scopeId=orgUnitId): Scoped to a specific org unit and its children
 */
@Getter
public class UserRole implements Entity<Long> {

    private Long id;
    private Long userId;
    private Long roleId;
    private String scopeType;  // ALL | ORG_UNIT
    private Long scopeId;      // 0 for ALL, orgUnitId for ORG_UNIT
    private LocalDateTime assignedAt;
    private Long assignedBy;
    private LocalDateTime expiresAt;
    private Boolean isActive;

    protected UserRole() {}

    @Builder
    public UserRole(Long id, Long userId, Long roleId, String scopeType, Long scopeId,
                    LocalDateTime assignedAt, Long assignedBy, LocalDateTime expiresAt,
                    Boolean isActive) {
        this.id = id;
        this.userId = userId;
        this.roleId = roleId;
        this.scopeType = scopeType != null ? scopeType : ScopeType.ALL;
        this.scopeId = scopeId != null ? scopeId : 0L;
        this.assignedAt = assignedAt != null ? assignedAt : LocalDateTime.now();
        this.assignedBy = assignedBy;
        this.expiresAt = expiresAt;
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * Assigns a role to a user with global scope (ALL).
     */
    public static UserRole assign(Long userId, Long roleId, Long assignedBy) {
        return UserRole.builder()
            .userId(userId)
            .roleId(roleId)
            .scopeType(ScopeType.ALL)
            .scopeId(0L)
            .assignedBy(assignedBy)
            .build();
    }

    /**
     * Assigns a role to a user with a specific scope.
     */
    public static UserRole assignWithScope(Long userId, Long roleId,
                                           String scopeType, Long scopeId, Long assignedBy) {
        if (!ScopeType.isValid(scopeType)) {
            throw new IllegalArgumentException("Invalid scope type: " + scopeType);
        }
        if (ScopeType.ORG_UNIT.equals(scopeType) && (scopeId == null || scopeId <= 0)) {
            throw new IllegalArgumentException("ORG_UNIT scope requires a valid scopeId");
        }
        return UserRole.builder()
            .userId(userId)
            .roleId(roleId)
            .scopeType(scopeType)
            .scopeId(ScopeType.ALL.equals(scopeType) ? 0L : scopeId)
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
     * Whether this is a global (ALL) scope assignment.
     */
    public boolean isGlobalScope() {
        return ScopeType.ALL.equals(scopeType);
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
