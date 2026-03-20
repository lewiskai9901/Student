package com.school.management.domain.access.event;

import com.school.management.domain.access.model.Role;
import com.school.management.domain.shared.event.BaseDomainEvent;
import lombok.Getter;

/**
 * Event raised when a new role is created.
 */
@Getter
public class RoleCreatedEvent extends BaseDomainEvent {

    private final String roleCode;
    private final String roleName;
    private final String roleType;

    public RoleCreatedEvent(Role role) {
        super("Role", role.getId() != null ? role.getId().toString() : "new");
        this.roleCode = role.getRoleCode();
        this.roleName = role.getRoleName();
        this.roleType = role.getRoleType();
    }

    /**
     * Returns the role ID.
     */
    public Long getRoleId() {
        String aggId = getAggregateId();
        return "new".equals(aggId) ? null : Long.valueOf(aggId);
    }

    @Override
    public String getEventType() {
        return "RoleCreatedEvent";
    }
}
