package com.school.management.domain.user.event;

import com.school.management.domain.shared.event.BaseDomainEvent;
import com.school.management.domain.user.model.valueobject.UserStatus;

/**
 * 用户状态变更事件
 */
public class UserStatusChangedEvent extends BaseDomainEvent {

    private final String userId;
    private final String username;
    private final UserStatus oldStatus;
    private final UserStatus newStatus;

    public UserStatusChangedEvent(String userId, String username, UserStatus oldStatus, UserStatus newStatus) {
        super("User", userId);
        this.userId = userId;
        this.username = username;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public UserStatus getOldStatus() {
        return oldStatus;
    }

    public UserStatus getNewStatus() {
        return newStatus;
    }
}
