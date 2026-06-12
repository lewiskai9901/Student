package com.school.management.application.place;

import com.school.management.application.access.AccessRelationService;
import com.school.management.application.access.AccessRelationService.GrantRequest;
import com.school.management.application.access.AccessRelationService.RevokeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 场所归属/责任人统一查询/写入入口 (对称 {@code MembershipResolver}).
 *
 * <p>场所归属统一表达为 access_relations 的 {@code belongs_to | place | org_unit} 关系,
 * <b>只存覆盖点</b> — 无关系 = 沿场所树继承父级; 每场所至多一条 (maxPerSubject=1,
 * DB 生成列 {@code uk_place_belongs_unique} 兜底)。解析后的有效组织物化在
 * {@code places.effective_org_unit_id} 投影列, 由 {@link PlaceOrgProjector} 维护。
 *
 * <p>场所责任人同构: {@code responsible_for | user | place} 关系覆盖点,
 * 每场所至多一人 (maxPerResource=1, {@code uk_place_responsible_unique} 兜底), 无投影列。
 *
 * <p>本服务是底层能力, <b>不做授权判断/数据权限 scope</b> (上层调用方负责); 单租户 (tenant=1)。
 * 写入语义是 grant-or-replace: 先撤旧覆盖点再建新覆盖点。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceOrgResolver {

    private static final String BELONGS_RELATION = "belongs_to";
    private static final String RESPONSIBLE_RELATION = "responsible_for";

    private final AccessRelationService accessRelationService;
    private final JdbcTemplate jdbcTemplate;

    // ═══════════════════════════════════════════════════════════════
    // 查询 — 归属覆盖点
    // ═══════════════════════════════════════════════════════════════

    /**
     * 该场所的<b>显式归属覆盖点</b> (不含继承). 继承态场所返回 empty;
     * 有效组织 (含继承) 请读 {@code places.effective_org_unit_id} 投影列.
     */
    public Optional<Long> orgOf(Long placeId) {
        if (placeId == null) return Optional.empty();
        List<Long> orgs = jdbcTemplate.queryForList(
            "SELECT resource_id FROM access_relations " +
            "WHERE subject_type = 'place' AND subject_id = ? " +
            "  AND resource_type = 'org_unit' AND relation = 'belongs_to' " +
            "  AND deleted = 0",
            Long.class, placeId);
        if (orgs.isEmpty()) return Optional.empty();
        if (orgs.size() > 1) {
            log.warn("[PlaceOrgResolver] place {} 有 {} 条活跃 belongs_to (应唯一), 取首条 {}",
                placeId, orgs.size(), orgs.get(0));
        }
        return Optional.of(orgs.get(0));
    }

    /** 批量取归属覆盖点: placeId → orgUnitId. 树构建用, 一条 IN 查询防 N+1. */
    public Map<Long, Long> overridesFor(Collection<Long> placeIds) {
        return batchOverrides(placeIds,
            "SELECT subject_id AS k, resource_id AS v FROM access_relations " +
            "WHERE subject_type = 'place' AND resource_type = 'org_unit' " +
            "  AND relation = 'belongs_to' AND deleted = 0 AND subject_id IN (%s)");
    }

    // ═══════════════════════════════════════════════════════════════
    // 查询 — 责任人覆盖点
    // ═══════════════════════════════════════════════════════════════

    /** 该场所的<b>显式责任人覆盖点</b> (不含继承). */
    public Optional<Long> responsibleOf(Long placeId) {
        if (placeId == null) return Optional.empty();
        List<Long> users = jdbcTemplate.queryForList(
            "SELECT subject_id FROM access_relations " +
            "WHERE resource_type = 'place' AND resource_id = ? " +
            "  AND subject_type = 'user' AND relation = 'responsible_for' " +
            "  AND deleted = 0",
            Long.class, placeId);
        if (users.isEmpty()) return Optional.empty();
        if (users.size() > 1) {
            log.warn("[PlaceOrgResolver] place {} 有 {} 个活跃 responsible_for (应唯一), 取首条 {}",
                placeId, users.size(), users.get(0));
        }
        return Optional.of(users.get(0));
    }

    /** 批量取责任人覆盖点: placeId → userId. */
    public Map<Long, Long> responsiblesFor(Collection<Long> placeIds) {
        return batchOverrides(placeIds,
            "SELECT resource_id AS k, subject_id AS v FROM access_relations " +
            "WHERE resource_type = 'place' AND subject_type = 'user' " +
            "  AND relation = 'responsible_for' AND deleted = 0 AND resource_id IN (%s)");
    }

    // ═══════════════════════════════════════════════════════════════
    // 写入 — grant-or-replace
    // ═══════════════════════════════════════════════════════════════

    /**
     * 设置场所归属覆盖点 (grant-or-replace). 投影列由 {@link PlaceOrgProjector}
     * 监听关系事件同事务重算, 调用方无需另行触发.
     */
    @Transactional
    public void setBelonging(Long placeId, Long orgUnitId) {
        if (placeId == null || orgUnitId == null) {
            throw new IllegalArgumentException("setBelonging 需要非空 placeId 与 orgUnitId");
        }
        clearBelonging(placeId);
        accessRelationService.forceGrant(GrantRequest.of(
            "place", placeId, BELONGS_RELATION, "org_unit", orgUnitId));
        log.info("[PlaceOrgResolver] setBelonging place {} -> org {}", placeId, orgUnitId);
    }

    /** 清除归属覆盖点 (场所回落为继承父级). 无覆盖点则 no-op. */
    @Transactional
    public void clearBelonging(Long placeId) {
        if (placeId == null) return;
        Optional<Long> current = orgOf(placeId);
        if (current.isEmpty()) return;
        accessRelationService.revoke(RevokeRequest.of(
            "place", placeId, BELONGS_RELATION, "org_unit", current.get(),
            "PlaceOrgResolver: 归属覆盖点变更/清除"));
        log.info("[PlaceOrgResolver] clearBelonging place {} (was org {})", placeId, current.get());
    }

    /** 设置场所责任人覆盖点 (grant-or-replace). */
    @Transactional
    public void setResponsible(Long placeId, Long userId) {
        if (placeId == null || userId == null) {
            throw new IllegalArgumentException("setResponsible 需要非空 placeId 与 userId");
        }
        clearResponsible(placeId);
        accessRelationService.forceGrant(GrantRequest.of(
            "user", userId, RESPONSIBLE_RELATION, "place", placeId));
        log.info("[PlaceOrgResolver] setResponsible place {} -> user {}", placeId, userId);
    }

    /** 清除责任人覆盖点 (回落为继承父级). 无覆盖点则 no-op. */
    @Transactional
    public void clearResponsible(Long placeId) {
        if (placeId == null) return;
        Optional<Long> current = responsibleOf(placeId);
        if (current.isEmpty()) return;
        accessRelationService.revoke(RevokeRequest.of(
            "user", current.get(), RESPONSIBLE_RELATION, "place", placeId,
            "PlaceOrgResolver: 责任人覆盖点变更/清除"));
        log.info("[PlaceOrgResolver] clearResponsible place {} (was user {})", placeId, current.get());
    }

    // ═══════════════════════════════════════════════════════════════

    private Map<Long, Long> batchOverrides(Collection<Long> placeIds, String sqlTemplate) {
        Map<Long, Long> result = new HashMap<>();
        if (placeIds == null || placeIds.isEmpty()) return result;
        String placeholders = String.join(",", java.util.Collections.nCopies(placeIds.size(), "?"));
        jdbcTemplate.query(String.format(sqlTemplate, placeholders),
            rs -> { result.put(rs.getLong("k"), rs.getLong("v")); },
            placeIds.toArray());
        return result;
    }
}
