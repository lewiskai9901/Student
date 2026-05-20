package com.school.management.application.system;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 插件测试沙箱的数据访问应用服务.
 *
 * <p>承载 {@code PluginSandboxController} 原先内联的所有 JdbcTemplate 逻辑,
 * 让 controller 回归纯接口层职责.
 */
@Service
@RequiredArgsConstructor
public class PluginSandboxApplicationService {

    private final JdbcTemplate jdbc;

    /**
     * 安全计数 — 任意异常降级为 0.
     */
    @Transactional(readOnly = true)
    public long countSafely(String sql) {
        try {
            Long n = jdbc.queryForObject(sql, Long.class);
            return n != null ? n : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 安全查询单个 id — 任意异常降级为 null.
     */
    @Transactional(readOnly = true)
    public Long queryIdSafely(String sql) {
        try {
            return jdbc.queryForObject(sql, Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 确保存在一个 [SANDBOX] 前缀的演示宿舍, 返回其 id.
     * 若不存在则插入 SANDBOX_D1 后再查回.
     */
    @Transactional
    public Long ensureSandboxDemoPlace() {
        Long placeId = queryIdSafely(
            "SELECT id FROM places WHERE place_name LIKE '[SANDBOX]%' AND deleted = 0 LIMIT 1");
        if (placeId == null) {
            jdbc.update(
                "INSERT INTO places (place_code, place_name, type_code, capacity, current_occupancy, " +
                "status, tenant_id, created_at) " +
                "VALUES ('SANDBOX_D1', '[SANDBOX] 沙箱测试宿舍', 'DORMITORY', 4, 0, 1, 1, NOW())");
            placeId = queryIdSafely(
                "SELECT id FROM places WHERE place_code = 'SANDBOX_D1' AND deleted = 0 LIMIT 1");
        }
        return placeId;
    }

    /**
     * 清理所有 [SANDBOX] 前缀的演示数据, 返回删除行数.
     */
    @Transactional
    public int resetSandboxData() {
        return jdbc.update("DELETE FROM places WHERE place_name LIKE '[SANDBOX]%'");
    }
}
