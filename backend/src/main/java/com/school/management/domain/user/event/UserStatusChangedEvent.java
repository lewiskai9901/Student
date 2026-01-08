package com.school.management.domain.user.event;

import com.school.management.domain.shared.event.DomainEvent;
import com.school.management.domain.user.model.valueobject.UserStatus;

import java.time.LocalDateTime;

/**
 * 用户状态变更事件
 */
public class UserStatusChangedEvent implements DomainEvent {

    private final String userId;
    private final String username;
    private final UserStatus oldStatus;
    private final UserStatus newStatus;
    private final LocalDateTime occurredOn;

    public UserStatusChangedEvent(String userId, String username, UserStatus oldStatus, UserStatus newStatus) {
        this.userId = userId;
        this.username = username;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public String aggregateId() {
        return userId;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public UserStatus getOldStatus() { return oldStatus; }
    public UserStatus getNewStatus() { return newStatus; }
}
