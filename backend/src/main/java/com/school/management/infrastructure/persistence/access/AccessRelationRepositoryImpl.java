package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.model.valueobject.AccessLevel;
import com.school.management.domain.access.repository.AccessRelationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AccessRelationRepositoryImpl implements AccessRelationRepository {

    private final AccessRelationMapper mapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<AccessRelation> findById(Long id) {
        AccessRelationPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<AccessRelation> listFiltered(String resourceType, String subjectType, String relation) {
        LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
                .orderByDesc(AccessRelationPO::getId);
        if (resourceType != null && !resourceType.isBlank())
            wrapper.eq(AccessRelationPO::getResourceType, resourceType);
        if (subjectType != null && !subjectType.isBlank())
            wrapper.eq(AccessRelationPO::getSubjectType, subjectType);
        if (relation != null && !relation.isBlank())
            wrapper.eq(AccessRelationPO::getRelation, relation);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AccessRelation> findByResource(String resourceType, Long resourceId) {
        LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
                .eq(AccessRelationPO::getResourceType, resourceType)
                .eq(AccessRelationPO::getResourceId, resourceId);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AccessRelation> findByResourceAndRelation(String resourceType, Long resourceId, String relation) {
        LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
                .eq(AccessRelationPO::getResourceType, resourceType)
                .eq(AccessRelationPO::getResourceId, resourceId)
                .eq(AccessRelationPO::getRelation, relation);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AccessRelation> findBySubject(String subjectType, Long subjectId) {
        LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
                .eq(AccessRelationPO::getSubjectType, subjectType)
                .eq(AccessRelationPO::getSubjectId, subjectId);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AccessRelation> findBySubjectAndResourceType(String subjectType, Long subjectId, String resourceType) {
        LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
                .eq(AccessRelationPO::getSubjectType, subjectType)
                .eq(AccessRelationPO::getSubjectId, subjectId)
                .eq(AccessRelationPO::getResourceType, resourceType);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean exists(String resourceType, Long resourceId, String relation, String subjectType, Long subjectId) {
        return mapper.existsRelation(resourceType, resourceId, relation, subjectType, subjectId);
    }

    @Override
    public List<Long> findAccessibleResourceIds(String resourceType, String subjectType, Long subjectId) {
        return mapper.findAccessibleResourceIds(resourceType, subjectType, subjectId);
    }

    @Override
    public List<Long> findAccessibleResourceIdsByOrgUnits(String resourceType, List<Long> orgUnitIds) {
        if (orgUnitIds == null || orgUnitIds.isEmpty()) return Collections.emptyList();
        return mapper.findAccessibleResourceIdsByOrgUnits(resourceType, orgUnitIds);
    }

    @Override
    public AccessRelation save(AccessRelation relation) {
        AccessRelationPO po = toPO(relation);
        mapper.insert(po);
        relation.setId(po.getId());
        return relation;
    }

    @Override
    public void update(AccessRelation relation) {
        AccessRelationPO po = toPO(relation);
        mapper.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByResource(String resourceType, Long resourceId) {
        LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
                .eq(AccessRelationPO::getResourceType, resourceType)
                .eq(AccessRelationPO::getResourceId, resourceId);
        mapper.delete(wrapper);
    }

    @Override
    public void deleteByResourceAndSubject(String resourceType, Long resourceId, String subjectType, Long subjectId) {
        LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
                .eq(AccessRelationPO::getResourceType, resourceType)
                .eq(AccessRelationPO::getResourceId, resourceId)
                .eq(AccessRelationPO::getSubjectType, subjectType)
                .eq(AccessRelationPO::getSubjectId, subjectId);
        mapper.delete(wrapper);
    }

    @Override
    public void deleteBySubject(String subjectType, Long subjectId) {
        LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
                .eq(AccessRelationPO::getSubjectType, subjectType)
                .eq(AccessRelationPO::getSubjectId, subjectId);
        mapper.delete(wrapper);
    }

    @Override
    public int batchSave(List<AccessRelation> relations) {
        int count = 0;
        for (AccessRelation rel : relations) {
            save(rel);
            count++;
        }
        return count;
    }

    @Override
    public int batchDeleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        return mapper.deleteBatchIds(ids);
    }

    // ═══════════════════════════════════════════════════════════════
    // W1.3 — Zanzibar 6 API 底层 SQL (从 AuthorizationService 收回)
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean checkDirectActive(String subjectType, Long subjectId,
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

    @Override
    public List<DirectRelationRef> findActiveDirectRelationsBySubject(
            String subjectType, Long subjectId, LocalDateTime at) {
        return jdbcTemplate.query(
            "SELECT resource_type, resource_id, relation FROM access_relations " +
            "WHERE subject_type = ? AND subject_id = ? " +
            "  AND deleted = 0 " +
            "  AND (valid_from IS NULL OR valid_from <= ?) " +
            "  AND (valid_to IS NULL OR valid_to > ?)",
            (rs, i) -> new DirectRelationRef(
                rs.getString("resource_type"),
                rs.getLong("resource_id"),
                rs.getString("relation")),
            subjectType, subjectId, at, at);
    }

    @Override
    public List<SubjectRefPO> expandSubjects(String resourceType, Long resourceId, String relation) {
        return jdbcTemplate.query(
            "SELECT subject_type, subject_id, access_level, valid_from, valid_to " +
            "FROM access_relations " +
            "WHERE resource_type = ? AND resource_id = ? AND relation = ? " +
            "  AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW())",
            (rs, i) -> new SubjectRefPO(
                rs.getString("subject_type"),
                rs.getLong("subject_id"),
                AccessLevel.parse(rs.getString("access_level")),
                toLdt(rs.getTimestamp("valid_from")),
                toLdt(rs.getTimestamp("valid_to"))),
            resourceType, resourceId, relation);
    }

    @Override
    public List<Long> findActiveSubjectIds(String resourceType, Long resourceId,
                                            String relation, String subjectTypeFilter) {
        String sql = "SELECT DISTINCT subject_id FROM access_relations " +
            "WHERE resource_type = ? AND resource_id = ? AND relation = ? " +
            "  AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW())" +
            (subjectTypeFilter != null ? " AND subject_type = ?" : "");
        return subjectTypeFilter != null
            ? jdbcTemplate.queryForList(sql, Long.class,
                resourceType, resourceId, relation, subjectTypeFilter)
            : jdbcTemplate.queryForList(sql, Long.class,
                resourceType, resourceId, relation);
    }

    @Override
    public List<RelationEdgeRef> lookupActiveResources(String subjectType, Long subjectId,
                                                        String relation, String resourceType) {
        // 注意: lookup 的原 SQL 只返 resource_id (List<Long>); 这里升级到带元数据 RelationEdgeRef.
        // 上层 lookup() 仍从 resource_id 抽列即可。
        return jdbcTemplate.query(
            "SELECT relation, subject_type, subject_id, resource_type, resource_id, " +
            "       access_level, valid_from, valid_to " +
            "FROM access_relations " +
            "WHERE subject_type = ? AND subject_id = ? " +
            "  AND relation = ? AND resource_type = ? " +
            "  AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW())",
            (rs, i) -> new RelationEdgeRef(
                rs.getString("relation"),
                rs.getString("subject_type"), rs.getLong("subject_id"),
                rs.getString("resource_type"), rs.getLong("resource_id"),
                AccessLevel.parse(rs.getString("access_level")),
                toLdt(rs.getTimestamp("valid_from")),
                toLdt(rs.getTimestamp("valid_to"))),
            subjectType, subjectId, relation, resourceType);
    }

    @Override
    public List<RelationEdgeRef> expandAllOnResource(String resourceType, Long resourceId) {
        return jdbcTemplate.query(
            "SELECT relation, subject_type, subject_id, access_level, valid_from, valid_to " +
            "FROM access_relations " +
            "WHERE resource_type = ? AND resource_id = ? " +
            "  AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW()) " +
            "ORDER BY relation, subject_type",
            (rs, i) -> new RelationEdgeRef(
                rs.getString("relation"),
                rs.getString("subject_type"), rs.getLong("subject_id"),
                resourceType, resourceId,
                AccessLevel.parse(rs.getString("access_level")),
                toLdt(rs.getTimestamp("valid_from")),
                toLdt(rs.getTimestamp("valid_to"))),
            resourceType, resourceId);
    }

    @Override
    public List<RelationEdgeRef> lookupAllForSubject(String subjectType, Long subjectId) {
        return jdbcTemplate.query(
            "SELECT resource_type, resource_id, relation, access_level, valid_from, valid_to " +
            "FROM access_relations " +
            "WHERE subject_type = ? AND subject_id = ? " +
            "  AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW()) " +
            "ORDER BY resource_type, relation",
            (rs, i) -> new RelationEdgeRef(
                rs.getString("relation"),
                subjectType, subjectId,
                rs.getString("resource_type"), rs.getLong("resource_id"),
                AccessLevel.parse(rs.getString("access_level")),
                toLdt(rs.getTimestamp("valid_from")),
                toLdt(rs.getTimestamp("valid_to"))),
            subjectType, subjectId);
    }

    @Override
    public boolean isRelationRegistered(String relation, String fromType, String toType) {
        Integer cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM relation_types " +
            "WHERE relation_code = ? AND from_type = ? AND to_type = ? AND is_enabled = 1",
            Integer.class, relation, fromType, toType);
        return cnt != null && cnt > 0;
    }

    @Override
    public Optional<Long> findActiveByTuple(String resourceType, Long resourceId,
                                             String relation,
                                             String subjectType, Long subjectId) {
        Long id = jdbcTemplate.query(
            "SELECT id FROM access_relations " +
            "WHERE resource_type = ? AND resource_id = ? AND relation = ? " +
            "  AND subject_type = ? AND subject_id = ? AND deleted = 0 " +
            "  AND (valid_to IS NULL OR valid_to > NOW()) " +
            "LIMIT 1",
            rs -> rs.next() ? rs.getLong("id") : null,
            resourceType, resourceId, relation, subjectType, subjectId);
        return Optional.ofNullable(id);
    }

    @Override
    public Long insertDirect(InsertDirectCommand cmd) {
        jdbcTemplate.update(
            "INSERT INTO access_relations " +
            "(resource_type, resource_id, relation, subject_type, subject_id, " +
            " include_children, access_level, valid_from, valid_to, metadata, remark, " +
            " tenant_id, created_by, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
            cmd.resourceType(), cmd.resourceId(), cmd.relation(),
            cmd.subjectType(), cmd.subjectId(),
            cmd.includeChildren() ? 1 : 0,
            cmd.accessLevel() != null ? cmd.accessLevel().name() : AccessLevel.FULL.name(),
            cmd.validFrom() != null ? cmd.validFrom() : LocalDateTime.now(),
            cmd.validTo(),
            cmd.metadataJson(), cmd.remark(),
            cmd.tenantId() != null ? cmd.tenantId() : 1L,
            cmd.createdBy());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Override
    public List<Long> findActiveIdsByTuple(String resourceType, Long resourceId,
                                            String relation,
                                            String subjectType, Long subjectId) {
        return jdbcTemplate.queryForList(
            "SELECT id FROM access_relations " +
            "WHERE resource_type = ? AND resource_id = ? AND relation = ? " +
            "  AND subject_type = ? AND subject_id = ? " +
            "  AND deleted = 0 AND (valid_to IS NULL OR valid_to > NOW())",
            Long.class,
            resourceType, resourceId, relation, subjectType, subjectId);
    }

    @Override
    public void archiveAndSoftDelete(Long id, String reason, Long actorId,
                                     String operatorIp, String userAgent) {
        // 1. INSERT … SELECT 归档到 history (Phase 7 W7.3: + operator_ip / operator_user_agent / operation)
        jdbcTemplate.update(
            "INSERT INTO access_relations_history " +
            "(original_id, resource_type, resource_id, relation, subject_type, subject_id, " +
            " include_children, access_level, valid_from, valid_to, metadata, remark, " +
            " archived_at, archived_reason, archived_by, operator_ip, operator_user_agent, operation, " +
            " tenant_id, created_by) " +
            "SELECT id, resource_type, resource_id, relation, subject_type, subject_id, " +
            "       include_children, access_level, valid_from, valid_to, metadata, remark, " +
            "       NOW(), ?, ?, ?, ?, 'REVOKE', tenant_id, created_by " +
            "FROM access_relations WHERE id = ?",
            reason, actorId, operatorIp, userAgent, id);

        // 2. 软删原表 + valid_to 截断到 NOW()
        jdbcTemplate.update(
            "UPDATE access_relations SET deleted = 1, deleted_at = NOW(), deleted_by = ?, " +
            "valid_to = COALESCE(valid_to, NOW()) " +
            "WHERE id = ?",
            actorId, id);
    }

    private static LocalDateTime toLdt(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    // ---------- 映射方法 ----------

    private AccessRelation toDomain(AccessRelationPO po) {
        Map<String, Object> meta = null;
        if (po.getMetadata() != null && !po.getMetadata().isEmpty()) {
            try {
                meta = objectMapper.readValue(po.getMetadata(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("解析 metadata JSON 失败, id={}: {}", po.getId(), e.getMessage());
            }
        }

        return AccessRelation.builder()
                .id(po.getId())
                .resourceType(po.getResourceType())
                .resourceId(po.getResourceId())
                .relation(po.getRelation())
                .subjectType(po.getSubjectType())
                .subjectId(po.getSubjectId())
                .includeChildren(Boolean.TRUE.equals(po.getIncludeChildren()))
                .accessLevel(AccessLevel.parse(po.getAccessLevel()))
                .metadata(meta)
                .validFrom(po.getValidFrom())
                .validTo(po.getValidTo())
                .remark(po.getRemark())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private AccessRelationPO toPO(AccessRelation domain) {
        AccessRelationPO po = new AccessRelationPO();
        po.setId(domain.getId());
        po.setResourceType(domain.getResourceType());
        po.setResourceId(domain.getResourceId());
        po.setRelation(domain.getRelation());
        po.setSubjectType(domain.getSubjectType());
        po.setSubjectId(domain.getSubjectId());
        po.setIncludeChildren(domain.isIncludeChildren());
        po.setAccessLevel(domain.getAccessLevel() != null ? domain.getAccessLevel().name() : AccessLevel.FULL.name());
        po.setValidFrom(domain.getValidFrom());
        po.setValidTo(domain.getValidTo());
        po.setRemark(domain.getRemark());
        po.setCreatedBy(domain.getCreatedBy());

        if (domain.getMetadata() != null && !domain.getMetadata().isEmpty()) {
            try {
                po.setMetadata(objectMapper.writeValueAsString(domain.getMetadata()));
            } catch (Exception e) {
                log.warn("序列化 metadata 失败: {}", e.getMessage());
            }
        }

        return po;
    }
}
