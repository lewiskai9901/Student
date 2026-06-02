package com.school.management.application.system;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Application service for menu-related data access.
 * Owns the user-permission lookup used by the menu API.
 */
@Service
@RequiredArgsConstructor
public class MenuQueryApplicationService {

    private final JdbcTemplate jdbc;

    /**
     * Load the distinct permission codes granted to a user via their roles.
     * Returns an empty set when the user id is null or the query fails.
     */
    @Transactional(readOnly = true)
    public Set<String> loadUserPermissions(Long userId) {
        if (userId == null) return Set.of();
        try {
            Set<String> perms = new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT p.permission_code FROM permissions p " +
                "JOIN role_permissions rp ON rp.permission_id = p.id " +
                "JOIN user_roles ur ON ur.role_id = rp.role_id " +
                "WHERE ur.user_id = ? AND p.deleted = 0 AND p.status = 1",
                String.class, userId));
            // 超管: 注入 system:admin + 通配 *, 让菜单过滤 (MenuController.filterAndSerialize)
            // 放行全部菜单, 不受"某菜单 requiredPermissions 无对应权限行"影响。
            // 与 CustomUserDetailsService 的超管全权一致。
            Integer isSuper = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id = ur.role_id " +
                "WHERE ur.user_id = ? AND r.role_code = 'SUPER_ADMIN'",
                Integer.class, userId);
            if (isSuper != null && isSuper > 0) {
                perms.add("system:admin");
                perms.add("*");
            }
            return perms;
        } catch (Exception e) {
            return Set.of();
        }
    }
}
