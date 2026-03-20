package com.school.management.domain.user.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 用户创建事件
 */
public class UserCreatedEvent extends BaseDomainEvent {

    private final String userId;
    private final String username;
    private final String realName;
    private final String userTypeCode;

    public UserCreatedEvent(String userId, String username, String realName, String userTypeCode) {
        super("User", userId);
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.userTypeCode = userTypeCode;
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

    public String getUserTypeCode() {
        return userTypeCode;
    }
}
