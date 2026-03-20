package com.school.management.domain.access.event;

import com.school.management.domain.shared.event.BaseDomainEvent;
import lombok.Getter;

/**
 * Event raised when a role is assigned to a user.
 */
@Getter
public class UserRoleAssignedEvent extends BaseDomainEvent {

    private final Long userId;
    private final Long roleId;
    private final String scopeType;
    private final Long scopeId;
    private final Long assignedBy;

    public UserRoleAssignedEvent(Long userId, Long roleId, String scopeType, Long scopeId, Long assignedBy) {
        super("UserRole", userId + "-" + roleId);
        this.userId = userId;
        this.roleId = roleId;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.assignedBy = assignedBy;
    }

    @Override
    public String getEventType() {
        return "UserRoleAssignedEvent";
    }
}
