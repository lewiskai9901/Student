package com.school.management.infrastructure.audit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志服务接口
 */
public interface AuditLogService {

    /**
     * 记录审计日志
     */
    void log(AuditLog auditLog);

    /**
     * 快捷方法：记录创建操作
     */
    void logCreate(String resourceType, String resourceId, String resourceName,
                   Object afterData, String description);

    /**
     * 快捷方法：记录更新操作
     */
    void logUpdate(String resourceType, String resourceId, String resourceName,
                   Object beforeData, Object afterData, String description);

    /**
     * 快捷方法：记录删除操作
     */
    void logDelete(String resourceType, String resourceId, String resourceName,
                   Object beforeData, String description);

    /**
     * 快捷方法：记录查看操作
     */
    void logRead(String resourceType, String resourceId, String resourceName);

    /**
     * 快捷方法：记录登录
     */
    void logLogin(Long userId, String username, boolean success, String errorMessage);

    /**
     * 快捷方法：记录登出
     */
    void logLogout(Long userId, String username);

    /**
     * 查询审计日志
     */
    List<AuditLog> query(AuditLogQuery query);

    /**
     * 统计操作次数
     */
    Map<String, Long> countByAction(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取用户最近操作
     */
    List<AuditLog> getRecentByUser(Long userId, int limit);

    /**
     * 获取资源操作历史
     */
    List<AuditLog> getResourceHistory(String resourceType, String resourceId);

    /**
     * 审计日志查询条件
     */
    @lombok.Data
    @lombok.Builder
    class AuditLogQuery {
        private Long userId;
        private String username;
        private AuditLog.AuditAction action;
        private String resourceType;
        private String resourceId;
        private AuditLog.AuditResult result;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String ipAddress;
        private Integer pageNum;
        private Integer pageSize;
    }
}
