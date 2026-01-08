package com.school.management.infrastructure.audit;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审计日志实体
 */
@Data
@Builder
public class AuditLog {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 操作类型
     */
    private AuditAction action;

    /**
     * 资源类型（如：USER, ROLE, CLASS, INSPECTION）
     */
    private String resourceType;

    /**
     * 资源ID
     */
    private String resourceId;

    /**
     * 资源名称
     */
    private String resourceName;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 变更前数据（JSON）
     */
    private String beforeData;

    /**
     * 变更后数据（JSON）
     */
    private String afterData;

    /**
     * 请求IP
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 请求URI
     */
    private String requestUri;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 操作结果
     */
    private AuditResult result;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 操作时间
     */
    private LocalDateTime occurredAt;

    /**
     * 额外信息
     */
    private Map<String, Object> metadata;

    /**
     * 审计操作类型
     */
    public enum AuditAction {
        CREATE("创建"),
        READ("查看"),
        UPDATE("更新"),
        DELETE("删除"),
        LOGIN("登录"),
        LOGOUT("登出"),
        EXPORT("导出"),
        IMPORT("导入"),
        APPROVE("审批"),
        REJECT("拒绝"),
        ASSIGN("分配"),
        REVOKE("撤销");

        private final String description;

        AuditAction(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 审计结果
     */
    public enum AuditResult {
        SUCCESS("成功"),
        FAILURE("失败"),
        PARTIAL("部分成功");

        private final String description;

        AuditResult(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
