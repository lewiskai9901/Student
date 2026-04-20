package com.school.management.application.access;

import com.school.management.infrastructure.extension.RelationTypePlugin.RelationTypeDef.Implied;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 发现 org_unit 下所有 member (user) id.
 *
 * 支持场景: 用户对 org_unit 有 admin/deputy 关系
 *   → 派生对该 org 下所有 member (user) 的 viewer 关系
 *   (主管自动能看到下属).
 *
 * 仅返回"直接成员", 子组织的成员不在内 — 需要跨组织层级传递时,
 * 上游关系本身应声明 transitive (沿 tree_path 展开), 或配合
 * {@link DescendantsOfOrgDiscovery} 先把关系派生到子组织再展开成员.
 */
@Component
@RequiredArgsConstructor
public class MembersOfOrgDiscovery implements RelationDiscoveryRule {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String code() {
        return Implied.MEMBERS_OF_ORG;
    }

    @Override
    public List<Long> discover(String fromResourceType, Long fromResourceId) {
        if (!"org_unit".equals(fromResourceType) || fromResourceId == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList(
            "SELECT DISTINCT subject_id FROM access_relations " +
            "WHERE resource_type = 'org_unit' AND resource_id = ? " +
            "  AND relation = 'member' " +
            "  AND subject_type = 'user' " +
            "  AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW())",
            Long.class, fromResourceId);
    }
}
