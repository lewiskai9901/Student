package com.school.management.domain.inspection.model.v7.platform;

import java.time.LocalDateTime;

/**
 * V7 审计日志读模型（不可变）
 * 记录检查系统内所有关键操作的审计追踪。
 * 审计条目一旦创建不可修改，因此不使用 AggregateRoot。
 */
public class AuditTrail {

    private Long id;
    private Long tenantId;
    private Long userId;
    private String userName;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String resourceName;
    /** JSON 格式的操作详情 */
    private String details;
    private String ipAddress;
    private LocalDateTime occurredAt;

    public AuditTrail() {}

    public static AuditTrail create(Long tenantId, Long userId, String userName,
                                     String action, String resourceType, Long resourceId,
                                     String resourceName, String details, String ipAddress) {
        AuditTrail trail = new AuditTrail();
        trail.tenantId = tenantId;
        trail.userId = userId;
        trail.userName = userName;
        trail.action = action;
        trail.resourceType = resourceType;
        trail.resourceId = resourceId;
        trail.resourceName = resourceName;
        trail.details = details;
        trail.ipAddress = ipAddress;
        trail.occurredAt = LocalDateTime.now();
        return trail;
    }

    // Getters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
