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

    // ─────────────── 按 feature 维度统计 (通用核心去行业表耦合) ───────────────

    /**
     * 统计带某 feature 的成员数 — 通用做法, 替代 {@code FROM user_student} / {@code user_type_code='TEACHER'}.
     *
     * <p>"学生/教师"语义不写死类型码, 改用用户类型 ({@code entity_type_configs}, entity_type='USER')
     * 的 {@code features} JSON 识别: 学生类型有 {@code isLearner}, 教师类型有 {@code canTeach}.
     * JSON 过滤语法与 {@link com.school.management.application.message.targetmode.ByFeatureTargetMode} 对齐
     * ({@code JSON_EXTRACT(features,'$.<key>') = true}).
     *
     * <p>统计口径 = 该组织的 member 归属 (access_relations) ∩ 用户类型具备该 feature.
     *
     * @param orgUnitId 组织; null 返回 0
     * @param featureKey feature 名 (白名单 [a-zA-Z0-9_]); 非法返回 0
     */
    public long countMembersByFeature(Long orgUnitId, String featureKey) {
        if (orgUnitId == null || !isValidFeatureKey(featureKey)) return 0L;
        Long cnt = jdbcTemplate.queryForObject(
            featureCountSql("ar.resource_id = ?", featureKey),
            Long.class, orgUnitId);
        return cnt != null ? cnt : 0L;
    }

    /**
     * 子树版: 统计 (org 本身 + 全部后代 org) 范围内带该 feature 的成员数 (跨 org 去重按 user).
     *
     * @param tenantId 租户
     * @param treePath 子树根的 tree_path (LIKE 前缀, 末尾自动补 '/' 由调用方保证或这里规范化)
     * @param featureKey feature 名
     */
    public long countMembersByFeatureInSubtree(Long tenantId, String treePath, String featureKey) {
        if (treePath == null || treePath.isBlank() || !isValidFeatureKey(featureKey)) return 0L;
        String prefix = treePath.endsWith("/") ? treePath : treePath + "/";
        Long cnt;
        if (tenantId == null) {
            // 单租户 / 无租户上下文 (如 impact 分析): 不加 tenant 过滤
            cnt = jdbcTemplate.queryForObject(
                featureCountSql(
                    "ar.resource_id IN (SELECT id FROM org_units " +
                    "WHERE tree_path LIKE ? AND deleted = 0)", featureKey),
                Long.class, prefix + "%");
        } else {
            cnt = jdbcTemplate.queryForObject(
                featureCountSql(
                    "ar.resource_id IN (SELECT id FROM org_units " +
                    "WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0)", featureKey),
                Long.class, tenantId, prefix + "%");
        }
        return cnt != null ? cnt : 0L;
    }

    /**
     * 子树版: 统计 (org 本身 + 全部后代 org) 范围内的<b>全部成员</b>数 (跨 org 按 user 去重, 不限 feature)。
     * 供 org-impact 严重度做通用计数 —— 核心不需要知道"学生/教师"这类行业 feature。
     */
    public long countMembersInSubtree(Long tenantId, String treePath) {
        if (treePath == null || treePath.isBlank()) return 0L;
        String prefix = treePath.endsWith("/") ? treePath : treePath + "/";
        Long cnt;
        if (tenantId == null) {
            cnt = jdbcTemplate.queryForObject(
                memberCountSql("ar.resource_id IN (SELECT id FROM org_units " +
                    "WHERE tree_path LIKE ? AND deleted = 0)"),
                Long.class, prefix + "%");
        } else {
            cnt = jdbcTemplate.queryForObject(
                memberCountSql("ar.resource_id IN (SELECT id FROM org_units " +
                    "WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0)"),
                Long.class, tenantId, prefix + "%");
        }
        return cnt != null ? cnt : 0L;
    }

    /** 同 featureCountSql 但不限 feature —— 统计 member 关系下的全部成员 (去重 user)。 */
    private String memberCountSql(String resourcePredicate) {
        return "SELECT COUNT(DISTINCT ar.subject_id) FROM access_relations ar " +
               "JOIN users u ON ar.subject_id = u.id " +
               "WHERE ar.relation = '" + RELATION + "' " +
               "  AND ar.resource_type = '" + RESOURCE_TYPE + "' " +
               "  AND ar.subject_type = '" + SUBJECT_TYPE + "' " +
               "  AND ar.deleted = 0 AND u.deleted = 0 AND u.status = 1 " +
               "  AND (ar.valid_to IS NULL OR ar.valid_to > NOW()) " +
               "  AND " + resourcePredicate;
    }

    /* countMembersByFeatureGlobal 已移除: 全局总数语义不应要求 org 归属, 改用 countUsersByFeature. */

    /**
     * 全局类型口径: 统计全系统带某 feature 的"用户"数 — 不要求 org 归属.
     *
     * <p>语义为"系统里有多少该类型 (具备此 feature) 的用户", 与 member 口径
     * ({@link #countMembersByFeature}/{@link #countMembersByFeatureInSubtree}) 不同:
     * 后者要求用户在某 org 有 member 归属关系, 用于"某组织/子树的学生数"; 而全局总数
     * 应把无任何组织归属的同类型用户也算进去.
     *
     * <p>统计口径 = 未删 + status=1 的 users ∩ 用户类型 ({@code entity_type_configs}, entity_type='USER')
     * 具备该 feature. 不 JOIN access_relations.
     *
     * @param featureKey feature 名 (白名单 [a-zA-Z0-9_]); 非法返回 0
     */
    public long countUsersByFeature(String featureKey) {
        if (!isValidFeatureKey(featureKey)) return 0L;
        Long cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users u " +
            "JOIN entity_type_configs etc ON etc.entity_type = 'USER' " +
            "  AND etc.type_code = u.user_type_code AND etc.deleted = 0 " +
            "  AND etc.is_enabled = 1 " +
            "WHERE u.deleted = 0 AND u.status = 1 " +
            "  AND JSON_EXTRACT(etc.features, '$." + featureKey + "') = true",
            Long.class);
        return cnt != null ? cnt : 0L;
    }

    /**
     * 拼 member ∩ feature 计数 SQL. {@code COUNT(DISTINCT ar.subject_id)} 防一个用户多 org 行重复计数
     * (归属虽唯一, 子树 union 仍按 user 去重最稳).
     */
    private String featureCountSql(String resourcePredicate, String featureKey) {
        return "SELECT COUNT(DISTINCT ar.subject_id) FROM access_relations ar " +
               "JOIN users u ON ar.subject_id = u.id " +
               "JOIN entity_type_configs etc ON etc.entity_type = 'USER' " +
               "  AND etc.type_code = u.user_type_code AND etc.deleted = 0 " +
               "  AND etc.is_enabled = 1 " +
               "WHERE ar.relation = '" + RELATION + "' " +
               "  AND ar.resource_type = '" + RESOURCE_TYPE + "' " +
               "  AND ar.subject_type = '" + SUBJECT_TYPE + "' " +
               "  AND ar.deleted = 0 AND u.deleted = 0 AND u.status = 1 " +
               "  AND (ar.valid_to IS NULL OR ar.valid_to > NOW()) " +
               "  AND JSON_EXTRACT(etc.features, '$." + featureKey + "') = true " +
               "  AND " + resourcePredicate;
    }

    /** feature 名白名单校验, 防 SQL 注入 (拼进 JSON_EXTRACT 路径). */
    private static boolean isValidFeatureKey(String featureKey) {
        return featureKey != null && featureKey.matches("[a-zA-Z0-9_]+");
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
