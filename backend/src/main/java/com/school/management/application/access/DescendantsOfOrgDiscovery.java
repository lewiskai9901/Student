package com.school.management.application.access;

import com.school.management.infrastructure.extension.RelationTypePlugin.RelationTypeDef.Implied;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 发现 org_unit 的所有子孙 org_unit id (不含自身).
 *
 * 支持场景: 用户对父 org 有 admin 关系
 *   → 派生对所有子 org 的 admin 关系 (跨层级管理).
 *
 * 实现: 基于 org_units.tree_path (形如 "/1/2/3/") 前缀匹配.
 * 与 {@code DataPermissionInterceptor} 的 SUBTREE 语义一致.
 */
@Component
@RequiredArgsConstructor
public class DescendantsOfOrgDiscovery implements RelationDiscoveryRule {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String code() {
        return Implied.DESCENDANTS_OF_ORG;
    }

    @Override
    public List<Long> discover(String fromResourceType, Long fromResourceId) {
        if (!"org_unit".equals(fromResourceType) || fromResourceId == null) {
            return List.of();
        }
        // 取根节点 tree_path, 再 LIKE 前缀匹配所有后代 (排除自身)
        List<String> paths = jdbcTemplate.queryForList(
            "SELECT tree_path FROM org_units WHERE id = ? AND deleted = 0",
            String.class, fromResourceId);
        if (paths.isEmpty() || paths.get(0) == null || paths.get(0).isBlank()) {
            return List.of();
        }
        String rootPath = paths.get(0);
        return jdbcTemplate.queryForList(
            "SELECT id FROM org_units WHERE tree_path LIKE ? AND id <> ? AND deleted = 0",
            Long.class, rootPath + "%", fromResourceId);
    }

    /**
     * 反向: 给定目标 org_unit id, 返回其所有祖先 org_unit id 列表 (不含自身).
     * 通过解析目标 org 的 tree_path ("/1/2/3/") 逐级拆分得到祖先 id.
     */
    @Override
    public List<Long> reverseDiscover(String targetResourceType, Long targetResourceId) {
        if (!"org_unit".equals(targetResourceType) || targetResourceId == null) {
            return List.of();
        }
        List<String> paths = jdbcTemplate.queryForList(
            "SELECT tree_path FROM org_units WHERE id = ? AND deleted = 0",
            String.class, targetResourceId);
        if (paths.isEmpty() || paths.get(0) == null || paths.get(0).isBlank()) {
            return List.of();
        }
        String treePath = paths.get(0);
        // tree_path 形如 "/1/2/3/" — 拆分得 [1, 2, 3], 排除自身 targetResourceId
        java.util.List<Long> ancestors = new java.util.ArrayList<>();
        for (String seg : treePath.split("/")) {
            if (seg.isEmpty()) continue;
            try {
                long id = Long.parseLong(seg);
                if (id != targetResourceId) ancestors.add(id);
            } catch (NumberFormatException ignored) {
                // tree_path 段不是数字 — 跳过
            }
        }
        return ancestors;
    }
}
