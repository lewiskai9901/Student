package com.school.management.application.access;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户角色控制器的数据访问应用服务.
 *
 * <p>从 {@code UserRoleController} 抽离 JdbcTemplate 访问 (scope 名称回显).
 */
@Service
@RequiredArgsConstructor
public class UserRoleJdbcApplicationService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 查 ORG_UNIT scope 的组织单元名称用于响应回显.
     * 查不到时返回 null, 不抛异常 — 与原控制器 catch-ignore 行为一致.
     */
    @Transactional(readOnly = true)
    public String findOrgUnitName(Long orgUnitId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT unit_name FROM org_units WHERE id = ? AND deleted = 0",
                    String.class, orgUnitId);
        } catch (Exception ignored) {
            return null;
        }
    }
}
