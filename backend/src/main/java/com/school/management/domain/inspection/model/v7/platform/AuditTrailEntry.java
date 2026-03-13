package com.school.management.domain.inspection.model.v7.platform;

import java.time.LocalDateTime;

/**
 * V7 审计日志条目 — 不可变记录
 * 不是聚合根，仅用于记录操作审计轨迹。
 * 创建后不可修改，只提供 getters（setId 除外，供持久化使用）。
 */
public class AuditTrailEntry {

    private Long id;
    private final Long tenantId;
    private final Long userId;
    private final String userName;
    /** 操作类型，如 "CREATE_TEMPLATE" / "PUBLISH_PROJECT" / "SUBMIT_INSPECTION" */
    private final String action;
    /** 资源类型，如 "InspTemplate" / "InspProject" / "InspTask" */
    private final String resourceType;
    private final Long resourceId;
    private final String resourceName;
    /** JSON，包含操作前后快照 */
    private final String details;
    private final String ipAddress;
    private final LocalDateTime occurredAt;

    public AuditTrailEntry(Long tenantId, Long userId, String userName,
                           String action, String resourceType, Long resourceId,
                           String resourceName, String details, String ipAddress,
                           LocalDateTime occurredAt) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.userName = userName;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.details = details;
        this.ipAddress = ipAddress;
        this.occurredAt = occurredAt != null ? occurredAt : LocalDateTime.now();
    }

    // Getters only (immutable)
    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public Long getResourceId() { return resourceId; }
    public String getResourceName() { return resourceName; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getOccurredAt() { return occurredAt; }

    /** 仅供持久化层设置 ID */
    public void setId(Long id) { this.id = id; }
}
