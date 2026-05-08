package com.school.management.application.access;

import com.school.management.infrastructure.extension.RelationTypeDef.Implied;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 发现 place 内所有 occupant (user) id — 参考实现。
 *
 * 支持场景: place 的 dorm_head / manages 关系 → 派生对 occupants 的 viewer/manager 关系。
 */
@Component
@RequiredArgsConstructor
public class OccupantsOfPlaceDiscovery implements RelationDiscoveryRule {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String code() {
        return Implied.OCCUPANTS_OF_PLACE;
    }

    @Override
    public List<Long> discover(String fromResourceType, Long fromResourceId) {
        if (!"place".equals(fromResourceType) || fromResourceId == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList(
            "SELECT DISTINCT subject_id FROM access_relations " +
            "WHERE resource_type = 'place' AND resource_id = ? " +
            "  AND relation IN ('occupies', 'occupant') " +
            "  AND subject_type = 'user' " +
            "  AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW())",
            Long.class, fromResourceId);
    }

    /**
     * 反向: 给定目标 user, 反查该 user 作为 occupant 所属的 place id 列表.
     */
    @Override
    public List<Long> reverseDiscover(String targetResourceType, Long targetResourceId) {
        if (!"user".equals(targetResourceType) || targetResourceId == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList(
            "SELECT DISTINCT resource_id FROM access_relations " +
            "WHERE subject_type = 'user' AND subject_id = ? " +
            "  AND resource_type = 'place' " +
            "  AND relation IN ('occupies', 'occupant') " +
            "  AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW())",
            Long.class, targetResourceId);
    }
}
