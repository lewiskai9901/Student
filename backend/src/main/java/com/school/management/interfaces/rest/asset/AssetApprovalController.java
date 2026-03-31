package com.school.management.interfaces.rest.asset;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Asset Approval REST Controller
 *
 * Uses JdbcTemplate to operate on: asset_approval
 */
@Slf4j
@RestController
@RequestMapping("/asset-approvals")
@RequiredArgsConstructor
public class AssetApprovalController {

    private final JdbcTemplate jdbc;

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

    // ==================== Create Approval ====================

    @PostMapping
    @Transactional
    public Result<Long> createApproval(@RequestBody Map<String, Object> data) {
        long id = IdWorker.getId();
        String approvalNo = "APR-" + System.currentTimeMillis();

        jdbc.update(
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
            toBigDecimal(data.get("applyAmount"))
        );

        return Result.success(id);
    }

    // ==================== Approve ====================

    @PostMapping("/{id}/approve")
    @Transactional
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> data) {
        String remark = data != null ? (String) data.get("remark") : null;
        jdbc.update(
            "UPDATE asset_approval SET status = 1, approval_remark = ?, approval_time = NOW(), " +
            "updated_at = NOW() WHERE id = ?",
            remark, id
        );
        return Result.success();
    }

    // ==================== Reject ====================

    @PostMapping("/{id}/reject")
    @Transactional
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> data) {
        String remark = data != null ? (String) data.get("remark") : null;
        jdbc.update(
            "UPDATE asset_approval SET status = 2, approval_remark = ?, approval_time = NOW(), " +
            "updated_at = NOW() WHERE id = ?",
            remark, id
        );
        return Result.success();
    }

    // ==================== Cancel ====================

    @PostMapping("/{id}/cancel")
    @Transactional
    public Result<Void> cancel(@PathVariable Long id) {
        jdbc.update(
            "UPDATE asset_approval SET status = 3, updated_at = NOW() WHERE id = ?", id);
        return Result.success();
    }

    // ==================== Get Approval ====================

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getApproval(@PathVariable Long id) {
        Map<String, Object> approval = jdbc.queryForMap(
            "SELECT " + APPROVAL_COLUMNS + " FROM asset_approval WHERE id = ?", id);
        enrichApproval(approval);
        return Result.success(approval);
    }

    // ==================== My Approvals ====================

    @GetMapping("/my")
    public Result<List<Map<String, Object>>> getMyApprovals() {
        List<Map<String, Object>> approvals = jdbc.queryForList(
            "SELECT " + APPROVAL_COLUMNS + " FROM asset_approval ORDER BY apply_time DESC"
        );
        for (Map<String, Object> a : approvals) enrichApproval(a);
        return Result.success(approvals);
    }

    // ==================== Pending Approvals ====================

    @GetMapping("/pending")
    public Result<List<Map<String, Object>>> getPendingApprovals() {
        List<Map<String, Object>> approvals = jdbc.queryForList(
            "SELECT " + APPROVAL_COLUMNS + " FROM asset_approval WHERE status = 0 ORDER BY apply_time DESC"
        );
        for (Map<String, Object> a : approvals) enrichApproval(a);
        return Result.success(approvals);
    }

    // ==================== Query Approvals (paginated) ====================

    @GetMapping
    public Result<Map<String, Object>> queryApprovals(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer approvalType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long applicantId) {

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

        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asset_approval" + where, Long.class, params.toArray());

        int offset = (pageNum - 1) * pageSize;
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbc.queryForList(
            "SELECT " + APPROVAL_COLUMNS + " FROM asset_approval" + where +
            " ORDER BY apply_time DESC LIMIT ? OFFSET ?",
            dataParams.toArray()
        );

        for (Map<String, Object> r : records) enrichApproval(r);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    // ==================== Pending Count ====================

    @GetMapping("/pending/count")
    public Result<Long> countPending() {
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asset_approval WHERE status = 0", Long.class);
        return Result.success(count);
    }

    // ==================== Helpers ====================

    private void enrichApproval(Map<String, Object> approval) {
        approval.put("approvalTypeDesc", getApprovalTypeDesc(approval.get("approvalType")));
        approval.put("statusDesc", getApprovalStatusDesc(approval.get("status")));
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

    private String getApprovalTypeDesc(Object type) {
        if (type == null) return null;
        int t = ((Number) type).intValue();
        switch (t) {
            case 1: return "借用申请";
            case 2: return "采购申请";
            case 3: return "报废申请";
            case 4: return "调拨申请";
            default: return "未知";
        }
    }

    private String getApprovalStatusDesc(Object status) {
        if (status == null) return null;
        int s = ((Number) status).intValue();
        switch (s) {
            case 0: return "待审批";
            case 1: return "已通过";
            case 2: return "已拒绝";
            case 3: return "已取消";
            default: return "未知";
        }
    }
}
