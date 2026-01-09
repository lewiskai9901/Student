package com.school.management.domain.user.event;

import com.school.management.domain.shared.event.BaseDomainEvent;
import com.school.management.domain.user.model.valueobject.UserType;

/**
 * 用户创建事件
 */
public class UserCreatedEvent extends BaseDomainEvent {

    private final String userId;
    private final String username;
    private final String realName;
    private final UserType userType;

    public UserCreatedEvent(String userId, String username, String realName, UserType userType) {
        super("User", userId);
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.userType = userType;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRealName() {
        return realName;
    }

    public UserType getUserType() {
        return userType;
    }
}
