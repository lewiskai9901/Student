package com.school.management.infrastructure.context;

import lombok.extern.slf4j.Slf4j;

/**
 * 请求上下文持有者
 * 使用ThreadLocal存储当前线程的请求上下文
 */
@Slf4j
public class RequestContextHolder {

    private static final ThreadLocal<RequestContext> contextHolder = new ThreadLocal<>();

    /**
     * 设置当前请求上下文
     */
    public static void setContext(RequestContext context) {
        contextHolder.set(context);
        log.debug("设置请求上下文: requestId={}, userId={}, userName={}",
                context.getRequestId(), context.getUserId(), context.getUserName());
    }

    /**
     * 获取当前请求上下文
     */
    public static RequestContext getContext() {
        RequestContext context = contextHolder.get();
        if (context == null) {
            // 如果没有上下文，返回系统默认上下文
            log.warn("当前线程没有请求上下文，返回系统默认上下文");
            return RequestContext.createSystemContext();
        }
        return context;
    }

    /**
     * 清除当前请求上下文
     * 重要：每次请求结束后必须调用，防止内存泄漏
     */
    public static void clearContext() {
        RequestContext context = contextHolder.get();
        if (context != null) {
            log.debug("清除请求上下文: requestId={}", context.getRequestId());
        }
        contextHolder.remove();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        return getContext().getUserId();
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUserName() {
        return getContext().getUserName();
    }

    /**
     * 获取当前请求ID
     */
    public static String getCurrentRequestId() {
        return getContext().getRequestId();
    }

    /**
     * 获取当前来源IP
     */
    public static String getCurrentSourceIp() {
        return getContext().getSourceIp();
    }
}
