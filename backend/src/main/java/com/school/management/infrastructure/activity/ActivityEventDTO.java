package com.school.management.infrastructure.activity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 活动事件响应 DTO
 */
@Data
public class ActivityEventDTO {

    private String id;
    private String requestId;

    private String resourceType;
    private String resourceId;
    private String resourceName;

    private String action;
    private String actionLabel;
    private String result;
    private String errorMessage;

    private List<FieldChangeDTO> changedFields;
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
    private Map<String, String> tags;

    private LocalDateTime occurredAt;

    @Data
    public static class FieldChangeDTO {
        private String fieldName;
        private String oldValue;
        private String newValue;
    }
}
