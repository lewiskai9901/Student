package com.school.management.util;

import com.school.management.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 *
 * @author system
 * @since 1.0.0
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户详情
     *
     * @return 用户详情，如果未登录或不是CustomUserDetails则返回null
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                return (CustomUserDetails) principal;
            }
        }
        return null;
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            // 处理 CustomUserDetails
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getUserId();
            }
            // 兼容字符串形式的用户ID
            if (principal instanceof String) {
                try {
                    return Long.parseLong((String) principal);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * 获取当前登录用户名
     *
     * @return 用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 检查当前用户是否有指定权限
     *
     * @param authority 权限
     * @return 是否有权限
     */
    public static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
        }
        return false;
    }

    /**
     * 检查当前用户是否有任意一个指定权限
     *
     * @param authorities 权限列表
     * @return 是否有权限
     */
    public static boolean hasAnyAuthority(String... authorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            for (String authority : authorities) {
                if (authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查当前用户是否有指定角色
     *
     * @param role 角色
     * @return 是否有角色
     */
    public static boolean hasRole(String role) {
        return hasAuthority("ROLE_" + role);
    }

    /**
     * 检查当前用户是否有任意一个指定角色
     *
     * @param roles 角色列表
     * @return 是否有角色
     */
    public static boolean hasAnyRole(String... roles) {
        String[] authorities = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            authorities[i] = "ROLE_" + roles[i];
        }
        return hasAnyAuthority(authorities);
    }

    /**
     * 检查是否已认证
     *
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * 检查是否为匿名用户
     *
     * @return 是否为匿名用户
     */
    public static boolean isAnonymous() {
        return !isAuthenticated();
    }
}