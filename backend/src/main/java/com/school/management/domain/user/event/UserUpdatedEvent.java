package com.school.management.domain.user.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 用户更新事件
 */
public class UserUpdatedEvent extends BaseDomainEvent {

    private final String userId;
    private final String username;
    private final String realName;

    public UserUpdatedEvent(String userId, String username, String realName) {
        super("User", userId);
        this.userId = userId;
        this.username = username;
        this.realName = realName;
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
}
