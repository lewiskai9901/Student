package com.school.management.application.access;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.infrastructure.extension.RelationTypePlugin.RelationTypeDef.Implied;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一授权服务 (Zanzibar 风格 6 API)
 *
 * 所有业务代码查询关系**必须**走这里,不要直接拼 SQL。
 * 语义:
 *   - check        (subject, relation, resource, at?): boolean
 *   - checkAt      同上,带时间点
 *   - expand       (resource, relation) → 所有 subject
 *   - lookup       (subject, relation, resource_type) → 所有 resource
 *   - grant        (subject, relation, resource, ...) → 创建关系 + 发事件
 *   - revoke       (subject, relation, resource, reason) → 软删 + 归档 + 发事件
 *   - batchCheck   (List<Check>)
 *
 * 时间语义:
 *   valid_from / valid_to 形成"生效窗口"。
 *   check() 默认只看"当前活跃"(valid_to IS NULL OR valid_to > NOW())。
 *   checkAt(..., timestamp) 看该时间点的快照。
 *
 * 软删语义:
 *   deleted = 1 → 数据纠错,彻底排除(包括历史查询)。
 *   valid_to 到期 → 业务失效,保留在主表/history 供审计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final List<RelationDiscoveryRule> discoveryRules;

    /**
     * 关系推导索引: (targetType + targetRelation) → 所有可能派生出它的
     * (sourceRelation, fromType, Implied) 三元组。启动加载, 变更时 refresh。
     */
    private final Map<String, List<ImpliedSource>> impliedIndex = new ConcurrentHashMap<>();
    /** discoveryRule code → 实现 */
    private final Map<String, RelationDiscoveryRule> discoveryByCode = new ConcurrentHashMap<>();

    @PostConstruct
    void bootImpliedCache() {
        for (RelationDiscoveryRule r : discoveryRules) {
            discoveryByCode.put(r.code(), r);
        }
        refreshImpliedCache();
        log.info("[Authorization] implied relation cache ready — {} entries, {} discovery rules",
            impliedIndex.size(), discoveryByCode.size());
    }

    /** 重新从 relation_types.implied_relations 加载索引 (DDL 变更后调用) */
    public void refreshImpliedCache() {
        Map<String, List<ImpliedSource>> fresh = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT relation_code, from_type, to_type, implied_relations FROM relation_types " +
            "WHERE is_enabled = 1 AND implied_relations IS NOT NULL");
        TypeReference<List<Implied>> typeRef = new TypeReference<>() {};
        for (Map<String, Object> row : rows) {
            String impliedJson = (String) row.get("implied_relations");
            if (impliedJson == null || impliedJson.isBlank()) continue;
            List<Implied> implied;
            try {
                implied = objectMapper.readValue(impliedJson, typeRef);
            } catch (Exception e) {
                log.warn("[Authorization] skip malformed implied_relations for {}: {}",
                    row.get("relation_code"), e.getMessage());
                continue;
            }
            String fromType = (String) row.get("from_type");
            String sourceRelation = (String) row.get("relation_code");
            String sourceToType = (String) row.get("to_type");
            for (Implied i : implied) {
                String key = impliedKey(i.targetType(), i.relation());
                fresh.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new ImpliedSource(fromType, sourceRelation, sourceToType, i));
            }
        }
        impliedIndex.clear();
        impliedIndex.putAll(fresh);
    }

    private static String impliedKey(String targetType, String relation) {
        return targetType + "#" + relation;
    }

    // ═══════════════════════════════════════════════════════════════
    // check / checkAt
    // ═══════════════════════════════════════════════════════════════

    /** 当前时间点:某主体对某资源是否有某关系? (包含 implied 推导) */
    public boolean check(String subjectType, Long subjectId,
                         String relation,
                         String resourceType, Long resourceId) {
        return checkAt(subjectType, subjectId, relation, resourceType, resourceId, LocalDateTime.now());
    }

    /** 指定时间点:某主体对某资源是否有某关系? (包含 implied 推导) */
    public boolean checkAt(String subjectType, Long subjectId,
                           String relation,
                           String resourceType, Long resourceId,
                           LocalDateTime at) {
        // 1. 直接关系
        if (checkDirect(subjectType, subjectId, relation, resourceType, resourceId, at)) {
            return true;
        }
        // 2. 展开 implied: 是否有任何"上游关系"能推导到 (resourceType, resourceId, relation) ?
        return checkImplied(subjectType, subjectId, relation, resourceType, resourceId, at);
    }

    /** 仅查直接写在 access_relations 表的关系 (不展开推导) */
    public boolean checkDirect(String subjectType, Long subjectId,
                               String relation,
                               String resourceType, Long resourceId,
                               LocalDateTime at) {
        Integer cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM access_relations " +
            "WHERE resource_type = ? AND resource_id = ? " +
            "  AND relation = ? " +
            "  AND subject_type = ? AND subject_id = ? " +
            "  AND deleted = 0 " +
            "  AND (valid_from IS NULL OR valid_from <= ?) " +
            "  AND (valid_to IS NULL OR valid_to > ?)",
            Integer.class,
            resourceType, resourceId, relation, subjectType, subjectId, at, at);
        return cnt != null && cnt > 0;
    }

    /**
     * 展开 implied: 对 target (resourceType, resourceId, relation) 查询,
     * 是否存在某条 "subject - 上游关系 - 上游 resource" 能通过 discoveryRule
     * 覆盖到本 resourceId。
     *
     * 算法 (MVP, 不做递归):
     *  1. 从 impliedIndex 查所有能派生到 (resourceType + relation) 的 ImpliedSource
     *  2. 对每个 source, 用 discoveryRule 的"逆向"能力检查 — MVP 实现是:
     *     遍历用户所有 "sourceRelation -> sourceToType resource" 的直接关系,
     *     对每条 resource 执行 discoveryRule.discover(), 看 resourceId 是否在返回列表里
     *  3. 命中任一即返 true
     */
    private boolean checkImplied(String subjectType, Long subjectId,
                                  String relation,
                                  String resourceType, Long resourceId,
                                  LocalDateTime at) {
        List<ImpliedSource> sources = impliedIndex.get(impliedKey(resourceType, relation));
        if (sources == null || sources.isEmpty()) return false;

        for (ImpliedSource src : sources) {
            // 只处理 subject_type 匹配的 source (派生关系的来源 subject_type == 当前 subjectType)
            if (!src.fromType.equals(subjectType)) continue;
            RelationDiscoveryRule rule = discoveryByCode.get(src.implied.discoveryRule());
            if (rule == null) {
                log.debug("[Authorization] implied 跳过 — unknown discoveryRule: {}",
                    src.implied.discoveryRule());
                continue;
            }
            // 查用户所有 "sourceRelation -> sourceToType" 的直接关系 resource_id
            List<Long> userUpstreamResources = jdbcTemplate.queryForList(
                "SELECT DISTINCT resource_id FROM access_relations " +
                "WHERE subject_type = ? AND subject_id = ? " +
                "  AND relation = ? AND resource_type = ? " +
                "  AND deleted = 0 " +
                "  AND (valid_from IS NULL OR valid_from <= ?) " +
                "  AND (valid_to IS NULL OR valid_to > ?)",
                Long.class,
                subjectType, subjectId, src.sourceRelation, src.sourceToType, at, at);
            for (Long upstreamResourceId : userUpstreamResources) {
                List<Long> discovered = rule.discover(src.sourceToType, upstreamResourceId);
                if (discovered != null && discovered.contains(resourceId)) {
                    log.debug("[Authorization] implied 命中: {}:{} via {} on {}:{} -> {}:{} {}",
                        subjectType, subjectId, src.sourceRelation, src.sourceToType, upstreamResourceId,
                        resourceType, resourceId, relation);
                    return true;
                }
            }
        }
        return false;
    }

    /** 内部类: 一条推导规则的源端信息 */
    private record ImpliedSource(String fromType, String sourceRelation,
                                  String sourceToType, Implied implied) {}

    /** 批量判定(消息扇出等批处理场景) */
    public Map<String, Boolean> batchCheck(List<CheckRequest> requests) {
        Map<String, Boolean> result = new LinkedHashMap<>(requests.size());
        // 简单实现:逐条查询;生产环境可优化为 IN 查询
        for (CheckRequest r : requests) {
            boolean ok = check(r.subjectType, r.subjectId, r.relation, r.resourceType, r.resourceId);
            result.put(r.key(), ok);
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // expand:某资源 + 某关系 → 所有 subject
    // ═══════════════════════════════════════════════════════════════

    /** 某资源 + 某关系 → 授权的 subject 列表(只返 active) */
    public List<SubjectRef> expand(String resourceType, Long resourceId, String relation) {
        return jdbcTemplate.query(
            "SELECT subject_type, subject_id, access_level, valid_from, valid_to " +
            "FROM access_relations " +
            "WHERE resource_type = ? AND resource_id = ? AND relation = ? " +
            "  AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW())",
            (rs, i) -> new SubjectRef(
                rs.getString("subject_type"),
                rs.getLong("subject_id"),
                rs.getString("access_level"),
                rs.getTimestamp("valid_from") != null ? rs.getTimestamp("valid_from").toLocalDateTime() : null,
                rs.getTimestamp("valid_to")   != null ? rs.getTimestamp("valid_to").toLocalDateTime()   : null
            ),
            resourceType, resourceId, relation);
    }

    /** 某资源上的所有关系(不限 relation) */
    public List<RelationEdge> expandAll(String resourceType, Long resourceId) {
        return jdbcTemplate.query(
            "SELECT relation, subject_type, subject_id, access_level, valid_from, valid_to " +
            "FROM access_relations " +
            "WHERE resource_type = ? AND resource_id = ? " +
            "  AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW()) " +
            "ORDER BY relation, subject_type",
            (rs, i) -> new RelationEdge(
                rs.getString("relation"),
                rs.getString("subject_type"), rs.getLong("subject_id"),
                resourceType, resourceId,
                rs.getString("access_level"),
                rs.getTimestamp("valid_from") != null ? rs.getTimestamp("valid_from").toLocalDateTime() : null,
                rs.getTimestamp("valid_to")   != null ? rs.getTimestamp("valid_to").toLocalDateTime()   : null
            ),
            resourceType, resourceId);
    }

    // ═══════════════════════════════════════════════════════════════
    // lookup:某 subject + 某关系 → 所有 resource
    // ═══════════════════════════════════════════════════════════════

    /** 某用户 + 某关系 + 某资源类型 → 用户能访问的资源 ID 列表 */
    public List<Long> lookup(String subjectType, Long subjectId,
                             String relation, String resourceType) {
        return jdbcTemplate.queryForList(
            "SELECT DISTINCT resource_id FROM access_relations " +
            "WHERE subject_type = ? AND subject_id = ? " +
            "  AND relation = ? AND resource_type = ? " +
            "  AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW())",
            Long.class,
            subjectType, subjectId, relation, resourceType);
    }

    /** 某用户所有关系(整张画像) */
    public List<RelationEdge> lookupAll(String subjectType, Long subjectId) {
        return jdbcTemplate.query(
            "SELECT resource_type, resource_id, relation, access_level, valid_from, valid_to " +
            "FROM access_relations " +
            "WHERE subject_type = ? AND subject_id = ? " +
            "  AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW()) " +
            "ORDER BY resource_type, relation",
            (rs, i) -> new RelationEdge(
                rs.getString("relation"),
                subjectType, subjectId,
                rs.getString("resource_type"), rs.getLong("resource_id"),
                rs.getString("access_level"),
                rs.getTimestamp("valid_from") != null ? rs.getTimestamp("valid_from").toLocalDateTime() : null,
                rs.getTimestamp("valid_to")   != null ? rs.getTimestamp("valid_to").toLocalDateTime()   : null
            ),
            subjectType, subjectId);
    }

    // ═══════════════════════════════════════════════════════════════
    // grant / revoke
    // ═══════════════════════════════════════════════════════════════

    /** 创建关系 → 发 RelationAssigned 事件 */
    @Transactional
    public Long grant(GrantRequest r) {
        // 1. 校验 relation_types 是否存在且启用
        Integer validRel = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM relation_types " +
            "WHERE relation_code = ? AND from_type = ? AND to_type = ? AND is_enabled = 1",
            Integer.class, r.relation, r.subjectType, r.resourceType);
        if (validRel == null || validRel == 0) {
            throw new IllegalArgumentException(String.format(
                "[Authorization] 非法 relation: %s %s -> %s (未注册或未启用)",
                r.relation, r.subjectType, r.resourceType));
        }

        // 2. 检查是否已有同样的活跃关系(幂等)
        Long existing = jdbcTemplate.query(
            "SELECT id FROM access_relations " +
            "WHERE resource_type = ? AND resource_id = ? AND relation = ? " +
            "  AND subject_type = ? AND subject_id = ? AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW()) " +
            "LIMIT 1",
            rs -> rs.next() ? rs.getLong("id") : null,
            r.resourceType, r.resourceId, r.relation, r.subjectType, r.subjectId);
        if (existing != null) {
            log.info("[Authorization] grant 幂等命中,返回现有关系 id={}", existing);
            return existing;
        }

        // 3. INSERT
        String metaJson;
        try {
            metaJson = r.metadata != null ? objectMapper.writeValueAsString(r.metadata) : null;
        } catch (Exception e) {
            metaJson = null;
        }
        jdbcTemplate.update(
            "INSERT INTO access_relations " +
            "(resource_type, resource_id, relation, subject_type, subject_id, " +
            " include_children, access_level, valid_from, valid_to, metadata, remark, " +
            " tenant_id, created_by, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
            r.resourceType, r.resourceId, r.relation, r.subjectType, r.subjectId,
            r.includeChildren ? 1 : 0,
            r.accessLevel != null ? r.accessLevel : "FULL",
            r.validFrom != null ? r.validFrom : LocalDateTime.now(),
            r.validTo,
            metaJson, r.remark,
            r.tenantId != null ? r.tenantId : 1L,
            r.grantedBy);

        Long newId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        log.info("[Authorization] grant: relation={} {} {}:{} -> {}:{} id={}",
            r.relation, r.subjectType, r.subjectId, r.resourceType, r.resourceId, r.accessLevel, newId);

        // 4. 发事件(本轮先打点,Day 8 再改用 TriggerService.fire)
        eventPublisher.publishEvent(new RelationAssignedEvent(
            newId, r.resourceType, r.resourceId, r.relation,
            r.subjectType, r.subjectId, r.grantedBy));

        return newId;
    }

    /** 撤销关系:软删 + 归档 + 发事件 */
    @Transactional
    public void revoke(RevokeRequest r) {
        // 1. 查到关系 id
        List<Long> ids = jdbcTemplate.queryForList(
            "SELECT id FROM access_relations " +
            "WHERE resource_type = ? AND resource_id = ? AND relation = ? " +
            "  AND subject_type = ? AND subject_id = ? " +
            "  AND deleted = 0 AND (valid_to IS NULL OR valid_to > NOW())",
            Long.class,
            r.resourceType, r.resourceId, r.relation, r.subjectType, r.subjectId);
        if (ids.isEmpty()) {
            log.warn("[Authorization] revoke 未找到活跃关系: {} {} -> {}:{}",
                r.relation, r.subjectId, r.resourceType, r.resourceId);
            return;
        }

        for (Long id : ids) {
            // 2. 归档到 history
            jdbcTemplate.update(
                "INSERT INTO access_relations_history " +
                "(original_id, resource_type, resource_id, relation, subject_type, subject_id, " +
                " include_children, access_level, valid_from, valid_to, metadata, remark, " +
                " archived_at, archived_reason, archived_by, tenant_id, created_by) " +
                "SELECT id, resource_type, resource_id, relation, subject_type, subject_id, " +
                "       include_children, access_level, valid_from, valid_to, metadata, remark, " +
                "       NOW(), ?, ?, tenant_id, created_by " +
                "FROM access_relations WHERE id = ?",
                r.reason, r.revokedBy, id);

            // 3. 软删
            jdbcTemplate.update(
                "UPDATE access_relations SET deleted = 1, deleted_at = NOW(), deleted_by = ?, " +
                "valid_to = COALESCE(valid_to, NOW()) " +
                "WHERE id = ?",
                r.revokedBy, id);

            // 4. 发事件
            eventPublisher.publishEvent(new RelationRevokedEvent(
                id, r.resourceType, r.resourceId, r.relation,
                r.subjectType, r.subjectId, r.reason, r.revokedBy));

            log.info("[Authorization] revoke id={} relation={} {} -> {}:{} reason={}",
                id, r.relation, r.subjectId, r.resourceType, r.resourceId, r.reason);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Data classes
    // ═══════════════════════════════════════════════════════════════

    public record SubjectRef(String subjectType, Long subjectId, String accessLevel,
                             LocalDateTime validFrom, LocalDateTime validTo) {}

    public record RelationEdge(String relation,
                               String subjectType, Long subjectId,
                               String resourceType, Long resourceId,
                               String accessLevel,
                               LocalDateTime validFrom, LocalDateTime validTo) {}

    public record CheckRequest(String subjectType, Long subjectId,
                               String relation,
                               String resourceType, Long resourceId) {
        public String key() {
            return String.format("%s:%d-[%s]->%s:%d", subjectType, subjectId, relation, resourceType, resourceId);
        }
    }

    public static class GrantRequest {
        public String subjectType; public Long subjectId;
        public String relation;
        public String resourceType; public Long resourceId;
        public String accessLevel;                      // READ_ONLY / FULL / OWNER
        public boolean includeChildren = false;
        public LocalDateTime validFrom; public LocalDateTime validTo;
        public Map<String, Object> metadata;
        public String remark;
        public Long grantedBy;
        public Long tenantId;

        public static GrantRequest of(String subjectType, Long subjectId,
                                      String relation,
                                      String resourceType, Long resourceId) {
            GrantRequest r = new GrantRequest();
            r.subjectType = subjectType; r.subjectId = subjectId;
            r.relation = relation;
            r.resourceType = resourceType; r.resourceId = resourceId;
            return r;
        }
    }

    public static class RevokeRequest {
        public String subjectType; public Long subjectId;
        public String relation;
        public String resourceType; public Long resourceId;
        public String reason;
        public Long revokedBy;

        public static RevokeRequest of(String subjectType, Long subjectId,
                                       String relation,
                                       String resourceType, Long resourceId,
                                       String reason) {
            RevokeRequest r = new RevokeRequest();
            r.subjectType = subjectType; r.subjectId = subjectId;
            r.relation = relation;
            r.resourceType = resourceType; r.resourceId = resourceId;
            r.reason = reason;
            return r;
        }
    }

    public record RelationAssignedEvent(Long relationId,
                                        String resourceType, Long resourceId,
                                        String relation,
                                        String subjectType, Long subjectId,
                                        Long grantedBy) {}

    public record RelationRevokedEvent(Long relationId,
                                       String resourceType, Long resourceId,
                                       String relation,
                                       String subjectType, Long subjectId,
                                       String reason, Long revokedBy) {}
}
