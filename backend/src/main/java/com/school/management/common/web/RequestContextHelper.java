package com.school.management.common.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前 HTTP request 上下文工具 — 抓 ip / user-agent (在 service 层用).
 *
 * <p>Phase 7 W7.3 引入, 给 grant/revoke 写 history.operator_* 字段.
 * 非 web 上下文 (定时任务 / 事件 listener / 单测) 调用时返回 null.
 */
@Slf4j
public class RequestContextHelper {

    private RequestContextHelper() {}

    /** 当前 HTTP 请求(可能 null,非 web 上下文如 scheduled job 调用). */
    public static HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 客户端 IP — 处理 X-Forwarded-For. null 当无 HTTP 上下文. */
    public static String clientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For 可能含多个 IP(代理链),取第一个
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        return req.getRemoteAddr();
    }

    public static String userAgent() {
        HttpServletRequest req = currentRequest();
        return req != null ? req.getHeader("User-Agent") : null;
    }

    /** 请求路径 (URI), null 当无 HTTP 上下文. */
    public static String requestUri() {
        HttpServletRequest req = currentRequest();
        return req != null ? req.getRequestURI() : null;
    }

    /** HTTP method (GET/POST/PUT/DELETE), null 当无 HTTP 上下文. */
    public static String httpMethod() {
        HttpServletRequest req = currentRequest();
        return req != null ? req.getMethod() : null;
    }

    /** request id (X-Request-Id / X-Trace-Id), null 当无 HTTP 上下文. */
    public static String requestId() {
        HttpServletRequest req = currentRequest();
        if (req == null) return null;
        String id = req.getHeader("X-Request-Id");
        if (id == null || id.isBlank()) id = req.getHeader("X-Trace-Id");
        return id;
    }
}
