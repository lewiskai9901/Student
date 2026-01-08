package com.school.management.domain.user.event;

import com.school.management.domain.shared.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * 用户密码重置事件
 */
public class UserPasswordResetEvent implements DomainEvent {

    private final String userId;
    private final String username;
    private final LocalDateTime occurredOn;

    public UserPasswordResetEvent(String userId, String username) {
        this.userId = userId;
        this.username = username;
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
}
