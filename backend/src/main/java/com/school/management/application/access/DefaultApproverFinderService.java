package com.school.management.application.access;

import com.school.management.domain.access.model.entity.PendingRelationApproval;
import com.school.management.infrastructure.extension.ApproverFinderPlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 默认审批人查找:申请人所在 org 的 admin.
 *
 * <p>优先级最低 (Integer.MAX_VALUE), 各行业插件可覆盖.
 *
 * <p>Phase 7 W7.4.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultApproverFinderService implements ApproverFinderPlugin {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<String> applicableRelations() {
        return null;  // fallback
    }

    @Override
    public List<Long> findApprovers(PendingRelationApproval p) {
        Long requesterId = p.getRequestedBy();
        if (requesterId == null) return Collections.emptyList();

        // 申请人 (p.requestedBy) 的 org_unit_id → 该 org 的 admin
        // 排除申请人自己, 避免自审
        try {
            List<Long> approvers = jdbcTemplate.queryForList(
                "SELECT ar.subject_id FROM users u " +
                "JOIN access_relations ar ON ar.resource_type='org_unit' " +
                "  AND ar.resource_id=u.org_unit_id AND ar.relation='admin' AND ar.deleted=0 " +
                "  AND (ar.valid_to IS NULL OR ar.valid_to > NOW()) " +
                "WHERE u.id=? AND ar.subject_id != u.id",
                Long.class, requesterId);

            log.debug("[Approver] 关系 {} 申请人 {} → {} 个候选",
                p.getRelation(), requesterId, approvers.size());
            return approvers;
        } catch (Exception e) {
            log.warn("[Approver] 查找审批人失败 (requester={}): {}", requesterId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }
}
