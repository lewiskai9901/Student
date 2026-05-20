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
            return new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT p.permission_code FROM permissions p " +
                "JOIN role_permissions rp ON rp.permission_id = p.id " +
                "JOIN user_roles ur ON ur.role_id = rp.role_id " +
                "WHERE ur.user_id = ? AND p.deleted = 0 AND p.status = 1",
                String.class, userId));
        } catch (Exception e) {
            return Set.of();
        }
    }
}
