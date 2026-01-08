package com.school.management.domain.user.event;

import com.school.management.domain.shared.event.DomainEvent;
import com.school.management.domain.user.model.valueobject.UserType;

import java.time.LocalDateTime;

/**
 * 用户创建事件
 */
public class UserCreatedEvent implements DomainEvent {

    private final String userId;
    private final String username;
    private final String realName;
    private final UserType userType;
    private final LocalDateTime occurredOn;

    public UserCreatedEvent(String userId, String username, String realName, UserType userType) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.userType = userType;
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
    public UserType getUserType() { return userType; }
}
