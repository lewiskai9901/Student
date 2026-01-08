package com.school.management.domain.access.event;

import com.school.management.domain.shared.event.DomainEvent;
import lombok.Getter;

/**
 * Event raised when a role is assigned to a user.
 */
@Getter
public class UserRoleAssignedEvent extends DomainEvent {

    private final Long userId;
    private final Long roleId;
    private final Long orgUnitId;
    private final Long assignedBy;

    public UserRoleAssignedEvent(Long userId, Long roleId, Long orgUnitId, Long assignedBy) {
        super("UserRole", userId + "-" + roleId);
        this.userId = userId;
        this.roleId = roleId;
        this.orgUnitId = orgUnitId;
        this.assignedBy = assignedBy;
    }

    @Override
    public String getEventType() {
        return "UserRoleAssignedEvent";
    }
}
