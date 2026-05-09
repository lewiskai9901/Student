package com.school.management.application.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.access.model.entity.PendingRelationApproval;
import com.school.management.domain.access.model.entity.PendingRelationApproval.Status;
import com.school.management.domain.access.model.valueobject.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 关系授权审批应用服务.
 *
 * <p>提供两步 grant 流:
 * 1. requestApproval — 写 pending_relation_approvals (status=PENDING)
 * 2. approve / reject  — 审批人决定,通过的调 AccessRelationService.applyApprovedRequest(...)
 *
 * <p>关系是否需要审批由 relation_types.approval_required 字段决定 (字典层).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelationApprovalService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /** 关系是否需要审批 — 查 relation_types 字典. */
    public boolean requiresApproval(String relationCode) {
        Integer flag = jdbcTemplate.queryForObject(
            "SELECT approval_required FROM relation_types " +
            "WHERE relation_code=? AND tenant_id=1 LIMIT 1",
            Integer.class, relationCode);
        return flag != null && flag == 1;
    }

    /** 提交审批申请, 返回 pending id */
    @Transactional
    public Long requestApproval(PendingRelationApproval p) {
        String metaJson;
        try {
            metaJson = p.getMetadata() != null ? objectMapper.writeValueAsString(p.getMetadata()) : null;
        } catch (Exception e) {
            metaJson = null;
        }
        jdbcTemplate.update(
            "INSERT INTO pending_relation_approvals " +
            "(resource_type, resource_id, relation, subject_type, subject_id, " +
            " access_level, valid_from, valid_to, metadata, remark, " +
            " status, requested_by, requested_at, tenant_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, NOW(), ?)",
            p.getResourceType(), p.getResourceId(), p.getRelation(),
            p.getSubjectType(), p.getSubjectId(),
            p.getAccessLevel() != null ? p.getAccessLevel().name() : AccessLevel.FULL.name(),
            p.getValidFrom(), p.getValidTo(),
            metaJson, p.getRemark(),
            p.getRequestedBy(),
            p.getTenantId() != null ? p.getTenantId() : 1L);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        log.info("[Approval] 申请 id={} relation={} subject={}:{} → resource={}:{} 待审批",
            id, p.getRelation(), p.getSubjectType(), p.getSubjectId(),
            p.getResourceType(), p.getResourceId());
        return id;
    }

    /** 审批通过 — 标记 status=APPROVED, 返回 pending 详情(由调用方调 grant 真正落库) */
    @Transactional
    public Optional<PendingRelationApproval> approve(Long pendingId, Long approverId) {
        Optional<PendingRelationApproval> p = findById(pendingId);
        if (p.isEmpty() || p.get().getStatus() != Status.PENDING) {
            log.warn("[Approval] approve 跳过 — id={} 状态不是 PENDING", pendingId);
            return Optional.empty();
        }
        jdbcTemplate.update(
            "UPDATE pending_relation_approvals SET status='APPROVED', approver_id=?, approved_at=NOW() " +
            "WHERE id=? AND status='PENDING'",
            approverId, pendingId);
        log.info("[Approval] approve id={} by user={}", pendingId, approverId);
        return p;
    }

    /** 审批拒绝 */
    @Transactional
    public void reject(Long pendingId, Long approverId, String reason) {
        jdbcTemplate.update(
            "UPDATE pending_relation_approvals SET status='REJECTED', approver_id=?, approved_at=NOW(), rejection_reason=? " +
            "WHERE id=? AND status='PENDING'",
            approverId, reason, pendingId);
        log.info("[Approval] reject id={} by user={} reason={}", pendingId, approverId, reason);
    }

    /** 审批通过后回填真实落库的 access_relations id, 形成可追溯链路. */
    @Transactional
    public void linkGrantedRelation(Long pendingId, Long grantedRelationId) {
        jdbcTemplate.update(
            "UPDATE pending_relation_approvals SET granted_relation_id=? WHERE id=?",
            grantedRelationId, pendingId);
    }

    /** 申请人撤回 */
    @Transactional
    public void cancel(Long pendingId, Long requesterId) {
        jdbcTemplate.update(
            "UPDATE pending_relation_approvals SET status='CANCELLED', approved_at=NOW() " +
            "WHERE id=? AND status='PENDING' AND requested_by=?",
            pendingId, requesterId);
    }

    /** 查询 pending 列表 (按 status 过滤, 默认所有 PENDING) */
    public List<PendingRelationApproval> listPending() {
        return jdbcTemplate.query(
            "SELECT id, resource_type, resource_id, relation, subject_type, subject_id, " +
            "  access_level, valid_from, valid_to, metadata, remark, status, " +
            "  requested_by, requested_at, approver_id, approved_at, rejection_reason, " +
            "  granted_relation_id, tenant_id " +
            "FROM pending_relation_approvals WHERE status='PENDING' ORDER BY requested_at DESC",
            (rs, i) -> mapRow(rs));
    }

    public Optional<PendingRelationApproval> findById(Long id) {
        List<PendingRelationApproval> rows = jdbcTemplate.query(
            "SELECT id, resource_type, resource_id, relation, subject_type, subject_id, " +
            "  access_level, valid_from, valid_to, metadata, remark, status, " +
            "  requested_by, requested_at, approver_id, approved_at, rejection_reason, " +
            "  granted_relation_id, tenant_id " +
            "FROM pending_relation_approvals WHERE id=?",
            (rs, i) -> mapRow(rs), id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private PendingRelationApproval mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        try {
            String metaJson = rs.getString("metadata");
            @SuppressWarnings("unchecked")
            java.util.Map<String,Object> meta = metaJson != null
                ? objectMapper.readValue(metaJson, java.util.Map.class)
                : null;
            return PendingRelationApproval.builder()
                .id(rs.getLong("id"))
                .resourceType(rs.getString("resource_type"))
                .resourceId(rs.getLong("resource_id"))
                .relation(rs.getString("relation"))
                .subjectType(rs.getString("subject_type"))
                .subjectId(rs.getLong("subject_id"))
                .accessLevel(AccessLevel.parse(rs.getString("access_level")))
                .validFrom(rs.getTimestamp("valid_from") != null ? rs.getTimestamp("valid_from").toLocalDateTime() : null)
                .validTo(rs.getTimestamp("valid_to") != null ? rs.getTimestamp("valid_to").toLocalDateTime() : null)
                .metadata(meta)
                .remark(rs.getString("remark"))
                .status(Status.valueOf(rs.getString("status")))
                .requestedBy(rs.getLong("requested_by"))
                .requestedAt(rs.getTimestamp("requested_at") != null ? rs.getTimestamp("requested_at").toLocalDateTime() : null)
                .approverId((Long) rs.getObject("approver_id"))
                .approvedAt(rs.getTimestamp("approved_at") != null ? rs.getTimestamp("approved_at").toLocalDateTime() : null)
                .rejectionReason(rs.getString("rejection_reason"))
                .grantedRelationId((Long) rs.getObject("granted_relation_id"))
                .tenantId(rs.getLong("tenant_id"))
                .build();
        } catch (Exception e) {
            throw new java.sql.SQLException("Failed to map approval row", e);
        }
    }
}
