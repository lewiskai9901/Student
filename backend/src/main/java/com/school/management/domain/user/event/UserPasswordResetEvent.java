package com.school.management.domain.user.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 用户密码重置事件
 */
public class UserPasswordResetEvent extends BaseDomainEvent {

    private final String userId;
    private final String username;

    public UserPasswordResetEvent(String userId, String username) {
        super("User", userId);
        this.userId = userId;
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
