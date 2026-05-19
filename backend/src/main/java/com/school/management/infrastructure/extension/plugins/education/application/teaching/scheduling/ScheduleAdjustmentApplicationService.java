package com.school.management.infrastructure.extension.plugins.education.application.teaching.scheduling;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调课应用服务 — 申请 / 审批 / 执行 / 取消 / 我的申请 / 待我审批
 *
 * 从 {@code TeachingScheduleController} 拆分而来 (M3.2.5 — 拆 service 不拆 controller).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleAdjustmentApplicationService {

    private final JdbcTemplate jdbc;

    /** 调课列表 (分页 + 状态过滤) */
    public Map<String, Object> listAdjustments(Integer status, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE deleted = 0");
        List<Object> params = new ArrayList<>();
        if (status != null) {
            where.append(" AND approval_status = ?");
            params.add(status);
        }

        String countSql = "SELECT COUNT(*) FROM schedule_adjustments" + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        String sql = "SELECT id, adjustment_code AS adjustmentCode, semester_id AS semesterId, " +
            "original_entry_id AS originalEntryId, adjustment_type AS adjustmentType, " +
            "original_date AS originalDate, original_weekday AS originalWeekday, " +
            "original_slot AS originalSlot, original_classroom_id AS originalClassroomId, " +
            "original_teacher_id AS originalTeacherId, " +
            "new_date AS newDate, new_weekday AS newWeekday, new_slot AS newSlot, " +
            "new_classroom_id AS newClassroomId, new_teacher_id AS newTeacherId, " +
            "applicant_id AS applicantId, apply_reason AS applyReason, apply_time AS applyTime, " +
            "approval_status AS approvalStatus, approver_id AS approverId, " +
            "approval_time AS approvalTime, approval_comment AS approvalComment, " +
            "executed, executed_at AS executedAt, " +
            "created_at AS createdAt " +
            "FROM schedule_adjustments" + where +
            " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        params.add(size);
        params.add((page - 1) * size);

        List<Map<String, Object>> records = jdbc.queryForList(sql, params.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    /** 调课详情 */
    public Map<String, Object> getAdjustment(Long id) {
        return jdbc.queryForMap(
            "SELECT id, adjustment_code AS adjustmentCode, semester_id AS semesterId, " +
            "original_entry_id AS originalEntryId, adjustment_type AS adjustmentType, " +
            "original_date AS originalDate, original_weekday AS originalWeekday, " +
            "original_slot AS originalSlot, original_classroom_id AS originalClassroomId, " +
            "original_teacher_id AS originalTeacherId, " +
            "new_date AS newDate, new_weekday AS newWeekday, new_slot AS newSlot, " +
            "new_classroom_id AS newClassroomId, new_teacher_id AS newTeacherId, " +
            "applicant_id AS applicantId, apply_reason AS applyReason, apply_time AS applyTime, " +
            "approval_status AS approvalStatus, approver_id AS approverId, " +
            "approval_time AS approvalTime, approval_comment AS approvalComment, " +
            "executed, executed_at AS executedAt, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM schedule_adjustments WHERE id = ? AND deleted = 0", id);
    }

    /** 提交调课申请 */
    public Map<String, Object> createAdjustment(Map<String, Object> data) {
        long id = IdWorker.getId();
        String adjustmentCode = "ADJ" + id;
        Long semesterId = data.get("semesterId") != null ? ((Number) data.get("semesterId")).longValue() : null;
        Long originalEntryId = data.get("originalEntryId") != null ? ((Number) data.get("originalEntryId")).longValue() : null;
        Integer adjustmentType = data.get("adjustmentType") != null ? ((Number) data.get("adjustmentType")).intValue() : null;

        String originalDateStr = (String) data.get("originalDate");
        LocalDate originalDate = originalDateStr != null ? LocalDate.parse(originalDateStr) : null;
        Integer originalWeekday = data.get("originalWeekday") != null ? ((Number) data.get("originalWeekday")).intValue() : null;
        Integer originalSlot = data.get("originalSlot") != null ? ((Number) data.get("originalSlot")).intValue() : null;
        Long originalClassroomId = data.get("originalClassroomId") != null ? ((Number) data.get("originalClassroomId")).longValue() : null;
        Long originalTeacherId = data.get("originalTeacherId") != null ? ((Number) data.get("originalTeacherId")).longValue() : null;

        String newDateStr = (String) data.get("newDate");
        LocalDate newDate = newDateStr != null ? LocalDate.parse(newDateStr) : null;
        Integer newWeekday = data.get("newWeekday") != null ? ((Number) data.get("newWeekday")).intValue() : null;
        Integer newSlot = data.get("newSlot") != null ? ((Number) data.get("newSlot")).intValue() : null;
        Long newClassroomId = data.get("newClassroomId") != null ? ((Number) data.get("newClassroomId")).longValue() : null;
        Long newTeacherId = data.get("newTeacherId") != null ? ((Number) data.get("newTeacherId")).longValue() : null;

        Long applicantId = data.get("applicantId") != null
                ? ((Number) data.get("applicantId")).longValue()
                : SecurityUtils.requireCurrentUserId();
        String applyReason = (String) data.get("applyReason");

        jdbc.update(
            "INSERT INTO schedule_adjustments (id, adjustment_code, semester_id, original_entry_id, " +
            "adjustment_type, original_date, original_weekday, original_slot, " +
            "original_classroom_id, original_teacher_id, " +
            "new_date, new_weekday, new_slot, new_classroom_id, new_teacher_id, " +
            "applicant_id, apply_reason, apply_time, " +
            "approval_status, executed, created_at, updated_at, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 0, 0, NOW(), NOW(), 0)",
            id, adjustmentCode, semesterId, originalEntryId,
            adjustmentType, originalDate, originalWeekday, originalSlot,
            originalClassroomId, originalTeacherId,
            newDate, newWeekday, newSlot, newClassroomId, newTeacherId,
            applicantId, applyReason
        );

        Map<String, Object> result = new HashMap<>(data);
        result.put("id", id);
        result.put("adjustmentCode", adjustmentCode);
        return result;
    }

    /** 审批通过 */
    public void approveAdjustment(Long id) {
        Long approverId = SecurityUtils.requireCurrentUserId();
        jdbc.update(
            "UPDATE schedule_adjustments SET approval_status = 1, approver_id = ?, " +
            "approval_time = NOW(), updated_at = NOW() WHERE id = ? AND deleted = 0",
            approverId, id);
    }

    /** 审批驳回 */
    public void rejectAdjustment(Long id, String comment) {
        Long approverId = SecurityUtils.requireCurrentUserId();
        jdbc.update(
            "UPDATE schedule_adjustments SET approval_status = 2, approver_id = ?, " +
            "approval_time = NOW(), approval_comment = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            approverId, comment, id);
    }

    /**
     * 执行调课 — 标记已执行 + 联动更新实况课表 (停课 / 调走)
     * 实况联动失败不影响主流程, 仅记 warn 日志.
     */
    public void executeAdjustment(Long id) {
        // 1. 标记已执行
        jdbc.update(
            "UPDATE schedule_adjustments SET executed = 1, executed_at = NOW(), " +
            "updated_at = NOW() WHERE id = ? AND deleted = 0", id);

        // 2. 联动更新实况课表
        try {
            Map<String, Object> adj = jdbc.queryForMap(
                "SELECT semester_id, original_entry_id, adjustment_type, " +
                "new_day_of_week, new_period_start, new_period_end, new_classroom_id, new_week " +
                "FROM schedule_adjustments WHERE id = ? AND deleted = 0", id);
            Long semesterId = ((Number) adj.get("semester_id")).longValue();
            Long entryId = adj.get("original_entry_id") != null ? ((Number) adj.get("original_entry_id")).longValue() : null;
            int adjType = ((Number) adj.get("adjustment_type")).intValue();

            if (entryId != null) {
                if (adjType == 2) {
                    // 停课: 取消对应实例
                    if (adj.get("new_week") != null) {
                        int week = ((Number) adj.get("new_week")).intValue();
                        jdbc.update(
                            "UPDATE schedule_instances SET status = 1, cancel_reason = '调课停课', source_id = ? " +
                            "WHERE entry_id = ? AND semester_id = ? AND week_number = ? AND status = 0 AND deleted = 0",
                            id, entryId, semesterId, week);
                    }
                } else if (adjType == 1) {
                    // 调课: 原实例标调走 + 生成新实例
                    if (adj.get("new_week") != null) {
                        int week = ((Number) adj.get("new_week")).intValue();
                        jdbc.update(
                            "UPDATE schedule_instances SET status = 2, cancel_reason = '已调课', source_id = ? " +
                            "WHERE entry_id = ? AND semester_id = ? AND week_number = ? AND status = 0 AND deleted = 0",
                            id, entryId, semesterId, week);
                    }
                    // 新实例在调课目标日期生成(如有新时间)
                }
            }
        } catch (Exception e) {
            log.warn("调课联动实况更新失败(非致命): {}", e.getMessage());
        }
    }

    /** 取消调课申请 */
    public void cancelAdjustment(Long id) {
        jdbc.update(
            "UPDATE schedule_adjustments SET approval_status = 3, " +
            "updated_at = NOW() WHERE id = ? AND deleted = 0", id);
    }

    /** 我提交的调课申请 (分页) */
    public Map<String, Object> myApplications(Integer status, int page, int size) {
        Long applicantId = SecurityUtils.requireCurrentUserId();
        StringBuilder where = new StringBuilder(" WHERE deleted = 0 AND applicant_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(applicantId);
        if (status != null) {
            where.append(" AND approval_status = ?");
            params.add(status);
        }

        String countSql = "SELECT COUNT(*) FROM schedule_adjustments" + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        String sql = "SELECT id, adjustment_code AS adjustmentCode, semester_id AS semesterId, " +
            "original_entry_id AS originalEntryId, adjustment_type AS adjustmentType, " +
            "original_date AS originalDate, original_weekday AS originalWeekday, " +
            "original_slot AS originalSlot, " +
            "new_date AS newDate, new_weekday AS newWeekday, new_slot AS newSlot, " +
            "applicant_id AS applicantId, apply_reason AS applyReason, apply_time AS applyTime, " +
            "approval_status AS approvalStatus, approval_comment AS approvalComment, " +
            "executed, created_at AS createdAt " +
            "FROM schedule_adjustments" + where +
            " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        params.add(size);
        params.add((page - 1) * size);

        List<Map<String, Object>> records = jdbc.queryForList(sql, params.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    /** 待审批列表 */
    public Map<String, Object> pendingApprovals(int page, int size) {
        String countSql = "SELECT COUNT(*) FROM schedule_adjustments WHERE deleted = 0 AND approval_status = 0";
        Long total = jdbc.queryForObject(countSql, Long.class);

        String sql = "SELECT id, adjustment_code AS adjustmentCode, semester_id AS semesterId, " +
            "original_entry_id AS originalEntryId, adjustment_type AS adjustmentType, " +
            "original_date AS originalDate, original_weekday AS originalWeekday, " +
            "original_slot AS originalSlot, " +
            "new_date AS newDate, new_weekday AS newWeekday, new_slot AS newSlot, " +
            "applicant_id AS applicantId, apply_reason AS applyReason, apply_time AS applyTime, " +
            "approval_status AS approvalStatus, " +
            "executed, created_at AS createdAt " +
            "FROM schedule_adjustments WHERE deleted = 0 AND approval_status = 0 " +
            "ORDER BY created_at DESC LIMIT ? OFFSET ?";

        List<Map<String, Object>> records = jdbc.queryForList(sql, size, (page - 1) * size);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }
}
