package com.school.management.domain.user.event;

import com.school.management.domain.shared.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * 用户更新事件
 */
public class UserUpdatedEvent implements DomainEvent {

    private final String userId;
    private final String username;
    private final String realName;
    private final LocalDateTime occurredOn;

    public UserUpdatedEvent(String userId, String username, String realName) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
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
    public String getRealName() { return realName; }
}
