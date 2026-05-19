package com.school.management.application.asset;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产审批应用服务 (M2, 2026-05-20).
 * AssetApprovalController 10 处直 jdbc 下沉.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetApprovalApplicationService {

    private final JdbcTemplate jdbcTemplate;

    private static final String APPROVAL_COLUMNS =
        "id, approval_no AS approvalNo, approval_type AS approvalType, " +
        "business_id AS businessId, asset_id AS assetId, asset_name AS assetName, " +
        "applicant_id AS applicantId, applicant_name AS applicantName, " +
        "applicant_dept AS applicantDept, " +
        "approver_id AS approverId, approver_name AS approverName, " +
        "status, apply_reason AS applyReason, " +
        "apply_quantity AS applyQuantity, apply_amount AS applyAmount, " +
        "approval_remark AS approvalRemark, " +
        "apply_time AS applyTime, approval_time AS approvalTime, " +
        "expire_time AS expireTime, created_at AS createdAt";

    @Transactional
    public long create(Map<String, Object> data) {
        long id = IdWorker.getId();
        String approvalNo = "APR-" + System.currentTimeMillis();
        jdbcTemplate.update(
            "INSERT INTO asset_approval (id, approval_no, approval_type, business_id, asset_id, " +
            "asset_name, applicant_id, applicant_name, status, apply_reason, " +
            "apply_quantity, apply_amount, apply_time) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, NOW())",
            id, approvalNo,
            data.get("approvalType"),
            toLong(data.get("businessId")),
            toLong(data.get("assetId")),
            data.get("assetName"),
            toLong(data.get("applicantId")),
            data.get("applicantName"),
            data.get("applyReason"),
            data.get("applyQuantity"),
            toBigDecimal(data.get("applyAmount")));
        return id;
    }

    @Transactional
    public void approve(Long id, String remark) {
        jdbcTemplate.update(
            "UPDATE asset_approval SET status = 1, approval_remark = ?, approval_time = NOW(), " +
            "updated_at = NOW() WHERE id = ?",
            remark, id);
    }

    @Transactional
    public void reject(Long id, String remark) {
        jdbcTemplate.update(
            "UPDATE asset_approval SET status = 2, approval_remark = ?, approval_time = NOW(), " +
            "updated_at = NOW() WHERE id = ?",
            remark, id);
    }

    @Transactional
    public void cancel(Long id) {
        jdbcTemplate.update(
            "UPDATE asset_approval SET status = 3, updated_at = NOW() WHERE id = ?", id);
    }

    public Map<String, Object> findById(Long id) {
        try {
            return jdbcTemplate.queryForMap(
                "SELECT " + APPROVAL_COLUMNS + " FROM asset_approval WHERE id = ?", id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Map<String, Object>> listAll() {
        return jdbcTemplate.queryForList(
            "SELECT " + APPROVAL_COLUMNS + " FROM asset_approval ORDER BY apply_time DESC");
    }

    public List<Map<String, Object>> listPending() {
        return jdbcTemplate.queryForList(
            "SELECT " + APPROVAL_COLUMNS + " FROM asset_approval WHERE status = 0 ORDER BY apply_time DESC");
    }

    public Map<String, Object> queryPaged(int pageNum, int pageSize,
                                          Integer approvalType, Integer status, Long applicantId) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (approvalType != null) {
            where.append(" AND approval_type = ?");
            params.add(approvalType);
        }
        if (status != null) {
            where.append(" AND status = ?");
            params.add(status);
        }
        if (applicantId != null) {
            where.append(" AND applicant_id = ?");
            params.add(applicantId);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset_approval" + where, Long.class, params.toArray());

        int offset = (pageNum - 1) * pageSize;
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(
            "SELECT " + APPROVAL_COLUMNS + " FROM asset_approval" + where +
            " ORDER BY apply_time DESC LIMIT ? OFFSET ?",
            dataParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    public long countPending() {
        Long c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset_approval WHERE status = 0", Long.class);
        return c == null ? 0 : c;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return null; }
    }
}
