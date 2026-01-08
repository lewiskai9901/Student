package com.school.management.domain.access.event;

import com.school.management.domain.access.model.Role;
import com.school.management.domain.shared.event.DomainEvent;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * Event raised when role permissions are changed.
 */
@Getter
public class RolePermissionsChangedEvent extends DomainEvent {

    private final String roleCode;
    private final Set<Long> addedPermissions;
    private final Set<Long> removedPermissions;

    public RolePermissionsChangedEvent(Role role, Set<Long> oldPermissions, Set<Long> newPermissions) {
        super("Role", role.getId() != null ? role.getId().toString() : "new");
        this.roleCode = role.getRoleCode();

        // Calculate added permissions
        this.addedPermissions = new HashSet<>(newPermissions);
        this.addedPermissions.removeAll(oldPermissions);

        // Calculate removed permissions
        this.removedPermissions = new HashSet<>(oldPermissions);
        this.removedPermissions.removeAll(newPermissions);
    }

    /**
     * Returns the role ID.
     */
    public Long getRoleId() {
        String aggId = getAggregateId();
        return "new".equals(aggId) ? null : Long.valueOf(aggId);
    }

    /**
     * Alias for getAddedPermissions for compatibility.
     */
    public Set<Long> getAddedPermissionIds() {
        return addedPermissions;
    }

    /**
     * Alias for getRemovedPermissions for compatibility.
     */
    public Set<Long> getRemovedPermissionIds() {
        return removedPermissions;
    }

    @Override
    public String getEventType() {
        return "RolePermissionsChangedEvent";
    }
}
