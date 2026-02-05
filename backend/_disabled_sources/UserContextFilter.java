package com.school.management.infrastructure.access;

import com.school.management.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户上下文过滤器 (V5)
 * 在每个请求中填充UserContext，供数据权限使用
 */
@Slf4j
@Component
public class UserContextFilter extends OncePerRequestFilter {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Set<String> SUPER_ADMIN_CODES = Set.of("SUPER_ADMIN", "ADMIN");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从Security上下文获取认证信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof UserPrincipal) {

                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                Long userId = userPrincipal.getId();

                // 构建用户上下文
                UserContext context = buildUserContext(userId, userPrincipal.getUsername());

                // 设置到ThreadLocal
                UserContextHolder.setContext(context);

                if (log.isDebugEnabled()) {
                    log.debug("UserContext set for user: {}, orgUnitId: {}, roles: {}",
                            context.getUsername(), context.getOrgUnitId(), context.getRoleCodes());
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            // 清理ThreadLocal，防止内存泄漏
            UserContextHolder.clear();
        }
    }

    /**
     * 构建用户上下文
     */
    private UserContext buildUserContext(Long userId, String username) {
        // 查询用户信息
        Map<String, Object> userInfo = getUserInfo(userId);

        Long orgUnitId = null;
        String orgUnitPath = null;

        if (userInfo != null) {
            orgUnitId = getLongValue(userInfo, "org_unit_id");

            // 查询组织路径
            if (orgUnitId != null) {
                orgUnitPath = getOrgUnitPath(orgUnitId);
            }
        }

        // 查询用户角色
        List<Map<String, Object>> roleInfos = getUserRoles(userId);

        List<Long> roleIds = roleInfos.stream()
                .map(r -> getLongValue(r, "role_id"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Set<String> roleCodes = roleInfos.stream()
                .map(r -> (String) r.get("role_code"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 判断是否为超级管理员
        boolean isSuperAdmin = roleCodes.stream().anyMatch(SUPER_ADMIN_CODES::contains);

        return UserContext.builder()
                .userId(userId)
                .username(username)
                .orgUnitId(orgUnitId)
                .orgUnitPath(orgUnitPath)
                .roleIds(roleIds)
                .roleCodes(roleCodes)
                .superAdmin(isSuperAdmin)
                .build();
    }

    private Map<String, Object> getUserInfo(Long userId) {
        try {
            String sql = "SELECT org_unit_id FROM users WHERE id = ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            log.warn("Failed to get user info for userId {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private String getOrgUnitPath(Long orgUnitId) {
        try {
            String sql = "SELECT path FROM org_units WHERE id = ?";
            return jdbcTemplate.queryForObject(sql, String.class, orgUnitId);
        } catch (Exception e) {
            log.warn("Failed to get org unit path for orgUnitId {}: {}", orgUnitId, e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> getUserRoles(Long userId) {
        try {
            String sql = "SELECT ur.role_id, r.role_code FROM user_roles ur " +
                    "JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = ?";
            return jdbcTemplate.queryForList(sql, userId);
        } catch (Exception e) {
            log.warn("Failed to get user roles for userId {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private Long getLongValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 排除静态资源和公开接口
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")
                || path.startsWith("/api/public/")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/druid");
    }
}
