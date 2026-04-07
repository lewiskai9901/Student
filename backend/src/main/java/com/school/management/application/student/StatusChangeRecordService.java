package com.school.management.application.student;

import com.school.management.common.PageResult;
import com.school.management.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生学籍异动记录服务
 * 使用 JdbcTemplate 直接操作 student_status_changes 表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatusChangeRecordService {

    private final JdbcTemplate jdbc;

    /**
     * 记录状态变更
     */
    @Transactional
    public void recordStatusChange(Long studentId, String changeType,
                                    String fromStatus, String toStatus,
                                    String reason) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();

        // 查询学生基本信息
        Map<String, Object> studentInfo = queryStudentInfo(studentId);
        String studentNo = (String) studentInfo.get("studentNo");
        String studentName = (String) studentInfo.get("name");

        jdbc.update(
            "INSERT INTO student_status_changes " +
            "(student_id, student_no, student_name, change_type, from_status, to_status, " +
            " reason, effective_date, operator_id, operator_name, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
            studentId, studentNo, studentName, changeType, fromStatus, toStatus,
            reason, LocalDate.now(), operatorId, operatorName
        );

        log.info("记录学籍异动: studentId={}, type={}, {} -> {}", studentId, changeType, fromStatus, toStatus);
    }

    /**
     * 记录转班
     */
    @Transactional
    public void recordTransfer(Long studentId, Long fromClassId, Long toClassId, String reason) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();

        Map<String, Object> studentInfo = queryStudentInfo(studentId);
        String studentNo = (String) studentInfo.get("studentNo");
        String studentName = (String) studentInfo.get("name");

        String fromClassName = resolveClassName(fromClassId);
        String toClassName = resolveClassName(toClassId);

        jdbc.update(
            "INSERT INTO student_status_changes " +
            "(student_id, student_no, student_name, change_type, from_status, to_status, " +
            " from_org_unit_id, from_class_name, to_org_unit_id, to_class_name, " +
            " reason, effective_date, operator_id, operator_name, created_at) " +
            "VALUES (?, ?, ?, 'TRANSFER_CLASS', 'STUDYING', 'STUDYING', ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
            studentId, studentNo, studentName,
            fromClassId, fromClassName, toClassId, toClassName,
            reason, LocalDate.now(), operatorId, operatorName
        );

        log.info("记录转班异动: studentId={}, {} -> {}", studentId, fromClassName, toClassName);
    }

    /**
     * 记录入学
     */
    @Transactional
    public void recordEnrollment(Long studentId, String studentNo, String studentName) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();

        jdbc.update(
            "INSERT INTO student_status_changes " +
            "(student_id, student_no, student_name, change_type, from_status, to_status, " +
            " reason, effective_date, operator_id, operator_name, created_at) " +
            "VALUES (?, ?, ?, 'ENROLL', NULL, 'STUDYING', '新生入学', ?, ?, ?, NOW())",
            studentId, studentNo, studentName,
            LocalDate.now(), operatorId, operatorName
        );

        log.info("记录入学异动: studentId={}, studentNo={}", studentId, studentNo);
    }

    /**
     * 查询某学生的所有异动记录
     */
    public List<Map<String, Object>> getStudentChanges(Long studentId) {
        return jdbc.queryForList(
            "SELECT id, student_id AS studentId, student_no AS studentNo, " +
            "student_name AS studentName, change_type AS changeType, " +
            "from_status AS fromStatus, to_status AS toStatus, " +
            "from_org_unit_id AS fromClassId, from_class_name AS fromClassName, " +
            "to_org_unit_id AS toClassId, to_class_name AS toClassName, " +
            "reason, effective_date AS effectiveDate, " +
            "operator_id AS operatorId, operator_name AS operatorName, " +
            "remark, created_at AS createdAt " +
            "FROM student_status_changes WHERE student_id = ? " +
            "ORDER BY created_at DESC",
            studentId
        );
    }

    /**
     * 分页查询所有异动记录（管理员视图）
     */
    public PageResult<Map<String, Object>> listRecentChanges(int page, int size, String changeType) {
        List<Object> params = new ArrayList<>();

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM student_status_changes WHERE 1=1");
        StringBuilder dataSql = new StringBuilder(
            "SELECT id, student_id AS studentId, student_no AS studentNo, " +
            "student_name AS studentName, change_type AS changeType, " +
            "from_status AS fromStatus, to_status AS toStatus, " +
            "from_org_unit_id AS fromClassId, from_class_name AS fromClassName, " +
            "to_org_unit_id AS toClassId, to_class_name AS toClassName, " +
            "reason, effective_date AS effectiveDate, " +
            "operator_id AS operatorId, operator_name AS operatorName, " +
            "remark, created_at AS createdAt " +
            "FROM student_status_changes WHERE 1=1"
        );

        if (changeType != null && !changeType.isEmpty()) {
            countSql.append(" AND change_type = ?");
            dataSql.append(" AND change_type = ?");
            params.add(changeType);
        }

        Long total = jdbc.queryForObject(countSql.toString(), Long.class, params.toArray());
        if (total == null) total = 0L;

        dataSql.append(" ORDER BY created_at DESC LIMIT ?, ?");
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add((page - 1) * size);
        dataParams.add(size);

        List<Map<String, Object>> records = jdbc.queryForList(dataSql.toString(), dataParams.toArray());

        return PageResult.of(records, total, page, size);
    }

    // ============ Helper methods ============

    private Map<String, Object> queryStudentInfo(Long studentId) {
        try {
            return jdbc.queryForMap(
                "SELECT s.student_no AS studentNo, u.real_name AS name " +
                "FROM students s LEFT JOIN users u ON s.user_id = u.id " +
                "WHERE s.id = ? AND s.deleted = 0",
                studentId
            );
        } catch (Exception e) {
            log.warn("查询学生信息失败: studentId={}", studentId, e);
            Map<String, Object> empty = new HashMap<>();
            empty.put("studentNo", null);
            empty.put("name", null);
            return empty;
        }
    }

    private String resolveClassName(Long orgUnitId) {
        if (orgUnitId == null) return null;
        try {
            return jdbc.queryForObject(
                "SELECT class_name FROM classes WHERE id = ? AND deleted = 0",
                String.class, orgUnitId
            );
        } catch (Exception e) {
            log.warn("查询班级名称失败: classId={}", orgUnitId, e);
            return null;
        }
    }
}
