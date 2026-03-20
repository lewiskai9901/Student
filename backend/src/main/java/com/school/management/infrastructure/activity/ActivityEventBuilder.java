package com.school.management.infrastructure.activity;

import com.school.management.domain.shared.model.valueobject.FieldChange;

import java.util.List;
import java.util.Map;

/**
 * 活动事件流式构建器
 * 自动填充 HTTP 上下文和用户信息，业务代码只需设置资源和变更
 */
public class ActivityEventBuilder {

    private final ActivityEventPublisher publisher;
    private final ActivityEvent.ActivityEventBuilder delegate;

    public ActivityEventBuilder(ActivityEventPublisher publisher, ActivityEvent.ActivityEventBuilder delegate) {
        this.publisher = publisher;
        this.delegate = delegate;
    }

    public ActivityEventBuilder resourceId(String resourceId) {
        delegate.resourceId(resourceId);
        return this;
    }

    public ActivityEventBuilder resourceId(Long resourceId) {
        delegate.resourceId(resourceId != null ? resourceId.toString() : null);
        return this;
    }

    public ActivityEventBuilder resourceName(String resourceName) {
        delegate.resourceName(resourceName);
        return this;
    }

    public ActivityEventBuilder actionLabel(String actionLabel) {
        delegate.actionLabel(actionLabel);
        return this;
    }

    public ActivityEventBuilder result(String result) {
        delegate.result(result);
        return this;
    }

    public ActivityEventBuilder errorMessage(String errorMessage) {
        delegate.errorMessage(errorMessage);
        return this;
    }

    public ActivityEventBuilder changedFields(List<FieldChange> changedFields) {
        delegate.changedFields(changedFields);
        return this;
    }

    public ActivityEventBuilder beforeSnapshot(String beforeSnapshot) {
        delegate.beforeSnapshot(beforeSnapshot);
        return this;
    }

    public ActivityEventBuilder afterSnapshot(String afterSnapshot) {
        delegate.afterSnapshot(afterSnapshot);
        return this;
    }

    public ActivityEventBuilder reason(String reason) {
        delegate.reason(reason);
        return this;
    }

    public ActivityEventBuilder tags(Map<String, String> tags) {
        delegate.tags(tags);
        return this;
    }

    /**
     * 构建并发布事件
     */
    public void publish() {
        publisher.publish(delegate.build());
    }

    /**
     * 仅构建事件，不发布
     */
    public ActivityEvent build() {
        return delegate.build();
    }
}
