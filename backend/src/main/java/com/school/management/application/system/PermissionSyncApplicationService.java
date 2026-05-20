package com.school.management.application.system;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for permission-sync data access.
 * Owns all SQL touching the {@code permissions} table for the sync-check utility.
 */
@Service
@RequiredArgsConstructor
public class PermissionSyncApplicationService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Load all active {@code permission_code} values from the permissions table.
     */
    @Transactional(readOnly = true)
    public List<String> loadPermissionCodes() {
        return jdbcTemplate.queryForList(
                "SELECT permission_code FROM permissions WHERE deleted = 0", String.class);
    }
}
