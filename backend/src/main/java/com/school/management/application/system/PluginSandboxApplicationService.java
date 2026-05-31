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
            // 沙箱演示场所只需任意一个已注册的场所类型 —— 不硬编码行业类型码 (如 DORMITORY),
            // 改为运行时从 entity_type_configs 取一个 place 维度的类型码, 取不到则用通用占位 PLACE.
            String typeCode = resolveAnyPlaceTypeCode();
            jdbc.update(
                "INSERT INTO places (place_code, place_name, type_code, capacity, current_occupancy, " +
                "status, tenant_id, created_at) " +
                "VALUES ('SANDBOX_D1', '[SANDBOX] 沙箱测试场所', ?, 4, 0, 1, 1, NOW())",
                typeCode);
            placeId = queryIdSafely(
                "SELECT id FROM places WHERE place_code = 'SANDBOX_D1' AND deleted = 0 LIMIT 1");
        }
        return placeId;
    }

    /** 运行时解析任意一个场所维度类型码, 解析失败降级为通用占位 "PLACE". */
    @Transactional(readOnly = true)
    public String resolveAnyPlaceTypeCode() {
        try {
            String code = jdbc.queryForObject(
                "SELECT type_code FROM entity_type_configs WHERE entity_type = 'PLACE' AND deleted = 0 LIMIT 1",
                String.class);
            return code != null ? code : "PLACE";
        } catch (Exception e) {
            return "PLACE";
        }
    }

    /**
     * 清理所有 [SANDBOX] 前缀的演示数据, 返回删除行数.
     */
    @Transactional
    public int resetSandboxData() {
        return jdbc.update("DELETE FROM places WHERE place_name LIKE '[SANDBOX]%'");
    }
}
