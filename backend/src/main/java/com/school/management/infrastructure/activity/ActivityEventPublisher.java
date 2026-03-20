package com.school.management.infrastructure.activity;

/**
 * 活动事件发布接口
 */
public interface ActivityEventPublisher {

    /**
     * 发布一个活动事件
     */
    void publish(ActivityEvent event);

    /**
     * 创建一个事件构建器（自动填充 HTTP 上下文和用户信息）
     */
    ActivityEventBuilder newEvent(String module, String resourceType, String action);

    /**
     * 创建一个事件构建器并指定 action label
     */
    default ActivityEventBuilder newEvent(String module, String resourceType, String action, String actionLabel) {
        return newEvent(module, resourceType, action).actionLabel(actionLabel);
    }
}
