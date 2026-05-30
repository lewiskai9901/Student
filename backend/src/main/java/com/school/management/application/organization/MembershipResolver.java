package com.school.management.application.organization;

import com.school.management.application.access.AccessRelationService;
import com.school.management.application.access.AccessRelationService.GrantRequest;
import com.school.management.application.access.AccessRelationService.RevokeRequest;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 组织归属统一查询/写入入口.
 *
 * <p>组织归属 (用户属于哪个组织) 统一表达为 access_relations 的
 * {@code member | user | org_unit} 关系, <b>每用户唯一</b> (DB 有 UNIQUE 约束兜底,
 * 且 {@code AccessRelationService.forceGrant} 已强制 maxPerSubject=1).
 *
 * <p>本服务是<b>全系统唯一的归属查询/写入入口</b> — 后续所有读写归属的代码都改走它,
 * 不再读 {@code users.primary_org_unit_id} 外键. 它本身是底层能力,
 * <b>不做数据权限 scope</b> (由上层调用方负责); 单租户 (tenant=1).
 *
 * <p>写入语义 ({@link #setMembership}) 是 grant-or-replace: 先撤旧归属再建新归属,
 * 因 maxPerSubject=1 已强制, 不先撤会触发基数违例. 新 member 行 {@code is_primary=1}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipResolver {

    private static final String SUBJECT_TYPE = "user";
    private static final String RESOURCE_TYPE = "org_unit";
    private static final String RELATION = "member";

    private final AccessRelationService accessRelationService;
    private final AccessRelationRepository accessRelationRepository;
    private final JdbcTemplate jdbcTemplate;
    private final OrgUnitRepository orgUnitRepository;

    // ═══════════════════════════════════════════════════════════════
    // 查询
    // ═══════════════════════════════════════════════════════════════

    /**
     * 反查用户唯一归属 org. (subject=user:userId, relation=member, resource_type=org_unit)
     * 活跃行取 resource_id. 唯一 (有 DB 约束兜底); 若历史脏数据多行, 取首条并告警.
     */
    public Optional<Long> orgOf(Long userId) {
        if (userId == null) return Optional.empty();
        List<Long> orgs = jdbcTemplate.queryForList(
            "SELECT DISTINCT resource_id FROM access_relations " +
            "WHERE subject_type = 'user' AND subject_id = ? " +
            "  AND resource_type = 'org_unit' AND relation = 'member' " +
            "  AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW())",
            Long.class, userId);
        if (orgs.isEmpty()) return Optional.empty();
        if (orgs.size() > 1) {
            log.warn("[MembershipResolver] user {} 有 {} 条活跃 member 归属 (应唯一), 取首条 {}",
                userId, orgs.size(), orgs.get(0));
        }
        return Optional.of(orgs.get(0));
    }

    /** 该组织的直接成员 userId 列表. */
    public List<Long> membersOf(Long orgUnitId) {
        if (orgUnitId == null) return List.of();
        return accessRelationRepository.findActiveSubjectIds(
            RESOURCE_TYPE, orgUnitId, RELATION, SUBJECT_TYPE);
    }

    /**
     * 该组织 + 全部子组织 的成员 userId 列表 (展开子树后去重).
     *
     * <p>H1: 必须真展开子树 — 取 org 本身 + 所有后代 org id, union 各 org 的直接成员.
     */
    public List<Long> membersOfSubtree(Long orgUnitId) {
        if (orgUnitId == null) return List.of();
        Optional<OrgUnit> root = orgUnitRepository.findById(orgUnitId);
        if (root.isEmpty()) return List.of();

        Set<Long> orgIds = new LinkedHashSet<>();
        orgIds.add(orgUnitId);
        String treePath = root.get().getTreePath();
        if (treePath != null && !treePath.isBlank()) {
            for (OrgUnit descendant : orgUnitRepository.findDescendants(treePath)) {
                if (descendant.getId() != null) orgIds.add(descendant.getId());
            }
        }

        Set<Long> members = new LinkedHashSet<>();
        for (Long orgId : orgIds) {
            members.addAll(accessRelationRepository.findActiveSubjectIds(
                RESOURCE_TYPE, orgId, RELATION, SUBJECT_TYPE));
        }
        return List.copyOf(members);
    }

    /** 该组织直接成员数. */
    public long countMembers(Long orgUnitId) {
        return membersOf(orgUnitId).size();
    }

    /**
     * 该组织直接成员按用户类型分组计数 — JOIN users 取 user_type_code.
     * 返回 {typeCode -> count}; user_type_code 为 NULL 归入 "UNKNOWN".
     */
    public Map<String, Long> countMembersByType(Long orgUnitId) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (orgUnitId == null) return result;
        List<Map.Entry<String, Long>> rows = jdbcTemplate.query(
            "SELECT u.user_type_code AS type_code, COUNT(*) AS cnt " +
            "FROM access_relations ar " +
            "JOIN users u ON ar.subject_id = u.id " +
            "WHERE ar.relation = 'member' AND ar.resource_type = 'org_unit' " +
            "  AND ar.resource_id = ? AND ar.subject_type = 'user' " +
            "  AND ar.deleted = 0 AND u.deleted = 0 " +
            "  AND (ar.valid_to IS NULL OR ar.valid_to > NOW()) " +
            "GROUP BY u.user_type_code",
            (rs, i) -> {
                String typeCode = rs.getString("type_code");
                return Map.entry(typeCode != null ? typeCode : "UNKNOWN", rs.getLong("cnt"));
            },
            orgUnitId);
        for (Map.Entry<String, Long> row : rows) {
            result.merge(row.getKey(), row.getValue(), Long::sum);
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // 写入
    // ═══════════════════════════════════════════════════════════════

    /**
     * 设置用户归属 (grant-or-replace, 保证唯一).
     * <ol>
     *   <li>{@link #clearMembership(Long)} 先撤掉该用户现有 member — 否则 forceGrant
     *       会因 maxPerSubject=1 抛 CardinalityViolationException.</li>
     *   <li>grant 新 member, 新行 {@code is_primary=1}.</li>
     * </ol>
     */
    @Transactional
    public void setMembership(Long userId, Long orgUnitId) {
        if (userId == null || orgUnitId == null) {
            throw new IllegalArgumentException("setMembership 需要非空 userId 与 orgUnitId");
        }
        // 1. revoke-before-grant — 解除现有归属 (maxPerSubject=1 已强制)
        clearMembership(userId);

        // 2. grant 新归属, is_primary=1
        GrantRequest req = GrantRequest.of(
            SUBJECT_TYPE, userId, RELATION, RESOURCE_TYPE, orgUnitId);
        req.isPrimary = true;
        accessRelationService.forceGrant(req);
        log.info("[MembershipResolver] setMembership user {} -> org {}", userId, orgUnitId);
    }

    /**
     * 撤销该用户的 member 归属. 无则 no-op.
     */
    @Transactional
    public void clearMembership(Long userId) {
        if (userId == null) return;
        Optional<Long> current = orgOf(userId);
        if (current.isEmpty()) return;
        accessRelationService.revoke(RevokeRequest.of(
            SUBJECT_TYPE, userId, RELATION, RESOURCE_TYPE, current.get(),
            "MembershipResolver: 归属变更/清除"));
        log.info("[MembershipResolver] clearMembership user {} (was org {})", userId, current.get());
    }
}
