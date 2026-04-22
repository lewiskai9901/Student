package com.school.management.common.util;

import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 插件生命周期前置护栏 — 写操作前验证资源所属插件未被禁用.
 * <p>
 * 禁用插件的贡献项 (plugin_enabled=0) 不允许 UPDATE / DELETE.
 * 管理员必须先启用所属插件, 再修改此类资源.
 * <p>
 * 配合 Phase 2 前端灰显 — 前端 disabled 是 UX 引导,
 * 后端 guard 是真正的数据一致性红线 (防 curl 直连绕过).
 *
 * <p>适用于 9 张贡献表:
 * roles / permissions / entity_type_configs / relation_types /
 * entity_event_types / event_triggers / trigger_points /
 * msg_subscription_rules / data_scope_dims
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginEnabledGuard {

    private final JdbcTemplate jdbc;

    /**
     * 校验给定表的指定 id 的 plugin_enabled=1, 否则抛 400.
     * 若 id 不存在 (让下游 404), 或表没有 plugin_enabled 列 (兼容), 通过.
     *
     * @param table 目标表 (已知白名单, 调用方负责不拼接用户输入)
     * @param id    资源主键
     */
    public void check(String table, Long id) {
        if (id == null) {
            return;
        }
        try {
            Integer v = jdbc.queryForObject(
                "SELECT plugin_enabled FROM " + table + " WHERE id = ?",
                Integer.class, id);
            if (v != null && v == 0) {
                throw new BusinessException("所属插件已禁用, 不可修改此资源. 请先启用所属插件.");
            }
        } catch (EmptyResultDataAccessException e) {
            // 资源不存在, 让下游返回 404
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 表无 plugin_enabled 列或其他查询问题 — 兼容, 通过
            log.debug("PluginEnabledGuard.check({}, {}) skipped: {}", table, id, e.getMessage());
        }
    }
}
