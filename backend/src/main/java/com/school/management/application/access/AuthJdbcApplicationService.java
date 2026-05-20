package com.school.management.application.access;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 认证流程的数据访问应用服务.
 *
 * <p>从 {@code AuthController} 抽离 JdbcTemplate 访问. 仅承载数据查询,
 * 不改动任何认证逻辑、参数、异常路径。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthJdbcApplicationService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * #7 用户端角色禁用提示: 查询用户所有 user_roles 绑定 (含 plugin_enabled=0 的被禁角色),
     * 不过滤, 让前端可渲染"插件禁用中"徽章.
     *
     * <p>行为与原 {@code AuthController.loadRoleDetails} 完全一致:
     * userId 为 null 或查询异常时返回空列表, 不抛出。
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> loadRoleDetails(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            return jdbcTemplate.queryForList(
                    "SELECT r.role_code AS code, r.role_name AS name, r.industry AS industry, " +
                    "       r.plugin_enabled AS pluginEnabled, r.status AS status " +
                    "FROM user_roles ur JOIN roles r ON r.id = ur.role_id " +
                    "WHERE ur.user_id = ? AND ur.is_active = 1 AND r.deleted = 0",
                    userId);
        } catch (Exception e) {
            log.warn("加载 roleDetails 失败 userId={}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
