package com.school.management.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.regex.Pattern;

/**
 * Web工具类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
public class WebUtils {

    // IP地址格式验证正则
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::$|^::1$|^([0-9a-fA-F]{1,4}:){1,7}:$");

    // 内网IP前缀
    private static final String[] PRIVATE_IP_PREFIXES = {
            "10.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.",
            "172.25.", "172.26.", "172.27.", "172.28.", "172.29.",
            "172.30.", "172.31.", "192.168.", "127."
    };

    /**
     * 获取当前请求
     *
     * @return HttpServletRequest
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取客户端IP地址（安全增强版）
     * 验证IP格式，检测可疑请求头
     *
     * @param request 请求对象
     * @return IP地址
     */
    public static String getIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = null;
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        // 检查X-Forwarded-For头
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // 对于多代理情况，取第一个IP
            if (xForwardedFor.contains(",")) {
                ip = xForwardedFor.substring(0, xForwardedFor.indexOf(",")).trim();
            } else {
                ip = xForwardedFor.trim();
            }

            // 验证X-Forwarded-For中的IP格式
            if (!isValidIpAddress(ip)) {
                log.warn("【安全警告】检测到可疑的X-Forwarded-For头: {}, 直连IP: {}",
                        xForwardedFor, request.getRemoteAddr());
                ip = null; // 回退到直连IP
            }
        }

        // 检查其他代理头
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = getValidIpFromHeader(request, "Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = getValidIpFromHeader(request, "WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = getValidIpFromHeader(request, "HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = getValidIpFromHeader(request, "HTTP_X_FORWARDED_FOR");
        }

        // 回退到直连IP
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 最终验证
        if (!isValidIpAddress(ip)) {
            log.warn("【安全警告】无法获取有效IP地址，使用unknown。RemoteAddr: {}", request.getRemoteAddr());
            return "unknown";
        }

        return ip;
    }

    /**
     * 从请求头获取并验证IP地址
     */
    private static String getValidIpFromHeader(HttpServletRequest request, String headerName) {
        String ip = request.getHeader(headerName);
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            if (isValidIpAddress(ip)) {
                return ip;
            } else {
                log.debug("无效的IP地址从头部 {}: {}", headerName, ip);
            }
        }
        return null;
    }

    /**
     * 验证IP地址格式是否有效
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip).matches() ||
               IPV6_PATTERN.matcher(ip).matches() ||
               "0:0:0:0:0:0:0:1".equals(ip) || // IPv6 localhost
               "::1".equals(ip);
    }

    /**
     * 检查是否为内网IP
     */
    public static boolean isPrivateIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        for (String prefix : PRIVATE_IP_PREFIXES) {
            if (ip.startsWith(prefix)) {
                return true;
            }
        }
        return "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip);
    }

    /**
     * 获取浏览器类型
     *
     * @param userAgent User-Agent字符串
     * @return 浏览器类型
     */
    public static String getBrowser(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("edge")) {
            return "Edge";
        } else if (userAgent.contains("chrome")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("safari")) {
            return "Safari";
        } else if (userAgent.contains("opera") || userAgent.contains("opr")) {
            return "Opera";
        } else if (userAgent.contains("msie") || userAgent.contains("trident")) {
            return "IE";
        }
        return "Unknown";
    }

    /**
     * 获取操作系统类型
     *
     * @param userAgent User-Agent字符串
     * @return 操作系统类型
     */
    public static String getOS(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("windows")) {
            return "Windows";
        } else if (userAgent.contains("mac")) {
            return "Mac OS";
        } else if (userAgent.contains("linux")) {
            return "Linux";
        } else if (userAgent.contains("android")) {
            return "Android";
        } else if (userAgent.contains("iphone") || userAgent.contains("ipad")) {
            return "iOS";
        }
        return "Unknown";
    }
}
