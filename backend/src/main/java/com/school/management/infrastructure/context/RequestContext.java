package com.school.management.infrastructure.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求上下文
 * 存储当前HTTP请求的审计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestContext {

    /**
     * 请求ID（用于关联同一请求的多个操作）
     */
    private String requestId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户类型：IAM_USER/SYSTEM
     */
    @Builder.Default
    private String userType = "IAM_USER";

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * JWT Token ID
     */
    private String accessKeyId;

    /**
     * 是否MFA认证
     */
    @Builder.Default
    private Boolean mfaAuthenticated = false;

    /**
     * 来源IP地址
     */
    private String sourceIp;

    /**
     * 用户代理（浏览器/客户端）
     */
    private String userAgent;

    /**
     * 来源页面URL（Referer）
     */
    private String referer;

    /**
     * API端点
     */
    private String apiEndpoint;

    /**
     * HTTP方法
     */
    private String httpMethod;

    /**
     * 请求时间（毫秒时间戳）
     */
    private Long requestTime;

    /**
     * 创建系统级上下文（用于定时任务等）
     */
    public static RequestContext createSystemContext() {
        return RequestContext.builder()
                .requestId(java.util.UUID.randomUUID().toString())
                .userId(0L)
                .userName("SYSTEM")
                .userType("SYSTEM")
                .sourceIp("127.0.0.1")
                .apiEndpoint("/system/internal")
                .httpMethod("SYSTEM")
                .requestTime(System.currentTimeMillis())
                .build();
    }
}
