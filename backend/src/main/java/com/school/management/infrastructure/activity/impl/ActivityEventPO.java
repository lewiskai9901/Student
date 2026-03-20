package com.school.management.infrastructure.activity.impl;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * activity_events 表持久化对象
 */
@Data
@TableName("activity_events")
public class ActivityEventPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String requestId;

    private String resourceType;
    private String resourceId;
    private String resourceName;

    private String action;
    private String actionLabel;
    private String result;
    private String errorMessage;

    /** JSON: [{fieldName, oldValue, newValue}] */
    private String changedFields;
    private String beforeSnapshot;
    private String afterSnapshot;

    private Long userId;
    private String userName;

    private String sourceIp;
    private String userAgent;
    private String apiEndpoint;
    private String httpMethod;

    private String reason;
    private String module;
    /** JSON: {key: value} */
    private String tags;

    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
}
