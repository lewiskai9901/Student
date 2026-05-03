package com.school.management.application.message.group;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * S+2: 通知组服务 — CRUD + 成员管理 + 把 GROUP 接收人解析成 user_id 列表.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MsgGroupService {

    private final JdbcTemplate jdbc;

    public List<Map<String, Object>> listGroups(Long tenantId) {
        return jdbc.queryForList(
                "SELECT g.id, g.group_code, g.group_name, g.description, g.enabled, "
                + "(SELECT COUNT(*) FROM msg_group_members m WHERE m.group_id = g.id AND m.deleted = 0) AS member_count "
                + "FROM msg_groups g WHERE g.tenant_id = ? AND g.deleted = 0 ORDER BY g.group_name",
                tenantId == null ? 0L : tenantId);
    }

    public Map<String, Object> getGroup(Long tenantId, Long id) {
        return jdbc.queryForMap(
                "SELECT id, group_code, group_name, description, enabled FROM msg_groups "
                + "WHERE id = ? AND tenant_id = ? AND deleted = 0",
                id, tenantId == null ? 0L : tenantId);
    }

    public List<Map<String, Object>> listMembers(Long tenantId, Long groupId) {
        return jdbc.queryForList(
                "SELECT m.user_id, u.real_name, u.username "
                + "FROM msg_group_members m JOIN users u ON u.id = m.user_id "
                + "WHERE m.group_id = ? AND m.deleted = 0 AND u.deleted = 0 "
                + "ORDER BY u.real_name",
                groupId);
    }

    @Transactional
    public Long createGroup(Long tenantId, String code, String name, String description, Long createdBy) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("group_code 不能为空");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("group_name 不能为空");
        jdbc.update(
                "INSERT INTO msg_groups (tenant_id, group_code, group_name, description, created_by) "
                + "VALUES (?, ?, ?, ?, ?)",
                tenantId == null ? 0L : tenantId, code, name, description, createdBy);
        return jdbc.queryForObject(
                "SELECT id FROM msg_groups WHERE tenant_id = ? AND group_code = ? AND deleted = 0",
                Long.class, tenantId == null ? 0L : tenantId, code);
    }

    @Transactional
    public void updateGroup(Long tenantId, Long id, String name, String description, Boolean enabled) {
        jdbc.update(
                "UPDATE msg_groups SET group_name = COALESCE(?, group_name), "
                + "description = COALESCE(?, description), enabled = COALESCE(?, enabled) "
                + "WHERE id = ? AND tenant_id = ? AND deleted = 0",
                name, description, enabled == null ? null : (enabled ? 1 : 0),
                id, tenantId == null ? 0L : tenantId);
    }

    @Transactional
    public void deleteGroup(Long tenantId, Long id) {
        jdbc.update("UPDATE msg_groups SET deleted = 1 WHERE id = ? AND tenant_id = ?",
                id, tenantId == null ? 0L : tenantId);
        jdbc.update("UPDATE msg_group_members SET deleted = 1 WHERE group_id = ?", id);
    }

    @Transactional
    public void addMembers(Long tenantId, Long groupId, List<Long> userIds, Long addedBy) {
        if (userIds == null || userIds.isEmpty()) return;
        for (Long uid : userIds) {
            try {
                jdbc.update(
                        "INSERT IGNORE INTO msg_group_members (tenant_id, group_id, user_id, added_by) "
                        + "VALUES (?, ?, ?, ?)",
                        tenantId == null ? 0L : tenantId, groupId, uid, addedBy);
            } catch (Exception e) {
                log.warn("[group] addMember 失败 groupId={} userId={}: {}", groupId, uid, e.getMessage());
            }
        }
    }

    @Transactional
    public void removeMember(Long groupId, Long userId) {
        jdbc.update("UPDATE msg_group_members SET deleted = 1 WHERE group_id = ? AND user_id = ?",
                groupId, userId);
    }

    /**
     * 把通知组解析成成员 user_id 列表 (MessageDispatcher 用).
     * 仅启用 (enabled=1) 的组才返回成员; 禁用组返回空表.
     */
    public List<Long> resolveGroupMembers(Long groupId) {
        List<Long> ids = jdbc.queryForList(
                "SELECT m.user_id FROM msg_group_members m JOIN msg_groups g ON g.id = m.group_id "
                + "WHERE m.group_id = ? AND m.deleted = 0 AND g.enabled = 1 AND g.deleted = 0",
                Long.class, groupId);
        return ids == null ? List.of() : ids;
    }
}
