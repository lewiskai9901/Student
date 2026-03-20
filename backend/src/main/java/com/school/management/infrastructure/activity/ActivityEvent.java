package com.school.management.infrastructure.activity;

import com.school.management.domain.shared.model.valueobject.FieldChange;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统一活动事件（不可变 POJO）
 */
@Getter
@Builder
public class ActivityEvent {

    private final String requestId;

    // 资源标识
    private final String resourceType;
    private final String resourceId;
    private final String resourceName;

    // 动作
    private final String action;
    private final String actionLabel;
    @Builder.Default
    private final String result = "SUCCESS";
    private final String errorMessage;

    // 变更追踪
    private final List<FieldChange> changedFields;
    private final String beforeSnapshot;
    private final String afterSnapshot;

    // 操作人
    private final Long userId;
    private final String userName;

    // HTTP 上下文
    private final String sourceIp;
    private final String userAgent;
    private final String apiEndpoint;
    private final String httpMethod;

    // 元数据
    private final String reason;
    private final String module;
    private final Map<String, String> tags;

    // 时间
    @Builder.Default
    private final LocalDateTime occurredAt = LocalDateTime.now();
}
