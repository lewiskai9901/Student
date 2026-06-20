package com.school.management.infrastructure.access;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.access.model.entity.RecordRelation;
import com.school.management.domain.access.repository.RecordRelationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * {@link RecordRelationRepository} 的 JdbcTemplate 实现 (统一锚定 R4 地基)。
 *
 * <p>风格对齐同域的 {@link DataPermissionPolicyService} (JdbcTemplate 直写, 非 MyBatis PO)。
 * {@code record_relations} 是承重承载表 —— 写失败直接抛, 不吞。
 */
@Slf4j
@Repository
public class RecordRelationRepositoryImpl implements RecordRelationRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecordRelationRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Long save(RecordRelation r) {
        Long tenantId = r.getTenantId() != null ? r.getTenantId() : 1L;
        String accessLevel = (r.getAccessLevel() != null && !r.getAccessLevel().isEmpty())
                ? r.getAccessLevel() : "READ_ONLY";
        String metaJson = toJson(r.getMetadata());

        // upsert: UK (resource,record,relation,subject,tenant,deleted) 下 deleted=0 行唯一 —— 撞则更新, 否则插入。
        Long existing = jdbc.query(
                "SELECT id FROM record_relations WHERE resource_code=? AND record_id=? AND relation_code=? " +
                "AND subject_type=? AND subject_id=? AND tenant_id=? AND deleted=0 LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null,
                r.getResourceCode(), r.getRecordId(), r.getRelationCode(),
                r.getSubjectType(), r.getSubjectId(), tenantId);

        if (existing != null) {
            jdbc.update(
                    "UPDATE record_relations SET access_level=?, valid_from=COALESCE(?, valid_from), valid_to=?, " +
                    "metadata=?, created_by=?, updated_at=NOW() WHERE id=?",
                    accessLevel, r.getValidFrom(), r.getValidTo(), metaJson, r.getCreatedBy(), existing);
            return existing;
        }

        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO record_relations (resource_code, record_id, relation_code, subject_type, subject_id, " +
                    "access_level, valid_from, valid_to, metadata, created_by, tenant_id, deleted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, COALESCE(?, NOW()), ?, ?, ?, ?, 0)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, r.getResourceCode());
            ps.setLong(2, r.getRecordId());
            ps.setString(3, r.getRelationCode());
            ps.setString(4, r.getSubjectType());
            ps.setLong(5, r.getSubjectId());
            ps.setString(6, accessLevel);
            ps.setObject(7, r.getValidFrom());
            ps.setObject(8, r.getValidTo());
            ps.setString(9, metaJson);
            ps.setObject(10, r.getCreatedBy());
            ps.setLong(11, tenantId);
            return ps;
        }, kh);
        return kh.getKey() != null ? kh.getKey().longValue() : null;
    }

    @Override
    public List<Long> findRecordIdsBySubject(String resourceCode, String relationCode,
                                             String subjectType, Long subjectId, Long tenantId) {
        return jdbc.queryForList(
                "SELECT record_id FROM record_relations WHERE resource_code=? AND relation_code=? " +
                "AND subject_type=? AND subject_id=? AND tenant_id=? AND deleted=0 " +
                "AND (valid_to IS NULL OR valid_to > NOW())",
                Long.class, resourceCode, relationCode, subjectType, subjectId, tenantId);
    }

    @Override
    public List<RecordRelation> findByRecordAndRelation(String resourceCode, Long recordId,
                                                        String relationCode, Long tenantId) {
        return jdbc.query(
                "SELECT * FROM record_relations WHERE resource_code=? AND record_id=? AND relation_code=? " +
                "AND tenant_id=? AND deleted=0",
                (rs, n) -> mapRow(rs),
                resourceCode, recordId, relationCode, tenantId);
    }

    @Override
    public int softDelete(String resourceCode, Long recordId, String relationCode,
                          String subjectType, Long subjectId, Long tenantId) {
        return jdbc.update(
                "UPDATE record_relations SET deleted=1, updated_at=NOW() WHERE resource_code=? AND record_id=? " +
                "AND relation_code=? AND subject_type=? AND subject_id=? AND tenant_id=? AND deleted=0",
                resourceCode, recordId, relationCode, subjectType, subjectId, tenantId);
    }

    private RecordRelation mapRow(ResultSet rs) throws SQLException {
        return RecordRelation.builder()
                .id(rs.getLong("id"))
                .resourceCode(rs.getString("resource_code"))
                .recordId(rs.getLong("record_id"))
                .relationCode(rs.getString("relation_code"))
                .subjectType(rs.getString("subject_type"))
                .subjectId(rs.getLong("subject_id"))
                .accessLevel(rs.getString("access_level"))
                .validFrom(toLdt(rs.getTimestamp("valid_from")))
                .validTo(toLdt(rs.getTimestamp("valid_to")))
                .metadata(parseMeta(rs.getString("metadata")))
                .createdBy((Long) rs.getObject("created_by"))
                .createdAt(toLdt(rs.getTimestamp("created_at")))
                .updatedAt(toLdt(rs.getTimestamp("updated_at")))
                .tenantId(rs.getLong("tenant_id"))
                .build();
    }

    private LocalDateTime toLdt(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private String toJson(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            log.warn("Failed to serialize record_relations metadata: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> parseMeta(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse record_relations metadata: {}", e.getMessage());
            return null;
        }
    }
}
