package com.school.management.infrastructure.extension.plugins.education.application.student;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.access.OrgScopeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学业预警应用服务 (M3.2.4, 2026-05-20).
 * AcademicWarningController 27 处直 jdbc 下沉.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicWarningApplicationService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final OrgScopeHelper orgScopeHelper;

    // ==================== Rules ====================

    public List<Map<String, Object>> listRules() {
        List<Map<String, Object>> rules = jdbc.queryForList(
            "SELECT id, rule_name AS ruleName, rule_type AS ruleType, " +
            "warning_level AS warningLevel, condition_params AS conditionParams, " +
            "applicable_grades AS applicableGrades, enabled, " +
            "created_at AS createdAt " +
            "FROM academic_warning_rules WHERE deleted = 0 ORDER BY warning_level DESC, id DESC"
        );
        for (Map<String, Object> rule : rules) {
            parseJsonField(rule, "conditionParams");
        }
        return rules;
    }

    /**
     * @return new rule id, or null if conditionParams unparseable.
     */
    @Transactional
    public Long createRule(Map<String, Object> data) {
        String ruleName = (String) data.get("ruleName");
        String ruleType = (String) data.get("ruleType");
        Integer warningLevel = ((Number) data.get("warningLevel")).intValue();
        Object conditionParams = data.get("conditionParams");
        String applicableGrades = (String) data.get("applicableGrades");
        Long currentUserId = SecurityUtils.getCurrentUserId();

        String paramsJson;
        try {
            paramsJson = conditionParams instanceof String
                ? (String) conditionParams
                : objectMapper.writeValueAsString(conditionParams);
        } catch (Exception e) {
            return null;
        }

        jdbc.update(
            "INSERT INTO academic_warning_rules (rule_name, rule_type, warning_level, condition_params, applicable_grades, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?)",
            ruleName, ruleType, warningLevel, paramsJson, applicableGrades, currentUserId
        );

        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /**
     * @return true if updated; false if conditionParams unparseable.
     */
    @Transactional
    public boolean updateRule(Long id, Map<String, Object> data) {
        String ruleName = (String) data.get("ruleName");
        String ruleType = (String) data.get("ruleType");
        Integer warningLevel = ((Number) data.get("warningLevel")).intValue();
        Object conditionParams = data.get("conditionParams");
        String applicableGrades = (String) data.get("applicableGrades");

        String paramsJson;
        try {
            paramsJson = conditionParams instanceof String
                ? (String) conditionParams
                : objectMapper.writeValueAsString(conditionParams);
        } catch (Exception e) {
            return false;
        }

        jdbc.update(
            "UPDATE academic_warning_rules SET rule_name = ?, rule_type = ?, warning_level = ?, " +
            "condition_params = ?, applicable_grades = ? WHERE id = ? AND deleted = 0",
            ruleName, ruleType, warningLevel, paramsJson, applicableGrades, id
        );
        return true;
    }

    @Transactional
    public void deleteRule(Long id) {
        jdbc.update("UPDATE academic_warning_rules SET deleted = 1 WHERE id = ?", id);
    }

    @Transactional
    public void toggleRule(Long id) {
        jdbc.update("UPDATE academic_warning_rules SET enabled = 1 - enabled WHERE id = ? AND deleted = 0", id);
    }

    // ==================== Scan ====================

    @Transactional
    public Map<String, Object> scanWarnings(Long semesterId) {
        List<Map<String, Object>> rules = jdbc.queryForList(
            "SELECT * FROM academic_warning_rules WHERE enabled = 1 AND deleted = 0");

        int totalWarnings = 0;

        for (Map<String, Object> rule : rules) {
            String ruleType = (String) rule.get("rule_type");
            int warningLevel = ((Number) rule.get("warning_level")).intValue();
            String paramsJson = rule.get("condition_params") != null ? rule.get("condition_params").toString() : "{}";

            Map<String, Object> params;
            try {
                params = objectMapper.readValue(paramsJson, new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("Failed to parse condition_params for rule {}: {}", rule.get("id"), e.getMessage());
                continue;
            }

            List<Map<String, Object>> flaggedStudents;

            switch (ruleType) {
                case "GRADE_FAIL": {
                    int minFailCount = getIntParam(params, "minFailCount", 2);
                    flaggedStudents = jdbc.queryForList(
                        "SELECT s.id AS student_id, s.student_no, s.name AS student_name, " +
                        "s.org_unit_id, sc.name AS class_name, " +
                        "COUNT(*) AS fail_count, " +
                        "GROUP_CONCAT(c.course_name SEPARATOR '、') AS failed_courses " +
                        "FROM student_grades sg " +
                        "JOIN user_student s ON s.id = sg.student_id " +
                        "LEFT JOIN school_classes sc ON sc.id = s.org_unit_id " +
                        "LEFT JOIN courses c ON c.id = sg.course_id " +
                        "WHERE sg.semester_id = ? AND sg.passed = 0 AND s.status = 1 " +
                        orgScopeHelper.orgScopeClause("s.org_unit_id") + " " +
                        "GROUP BY s.id, s.student_no, s.name, s.org_unit_id, sc.name " +
                        "HAVING fail_count >= ?",
                        semesterId, minFailCount);

                    for (Map<String, Object> student : flaggedStudents) {
                        String desc = String.format("本学期挂科%s门: %s",
                            student.get("fail_count"), student.get("failed_courses"));
                        insertWarning(semesterId, student, rule, "GRADE_FAIL", warningLevel, desc, student);
                        totalWarnings++;
                    }
                    break;
                }
                case "ATTENDANCE_LOW": {
                    int minRate = getIntParam(params, "minAttendanceRate", 80);
                    flaggedStudents = jdbc.queryForList(
                        "SELECT s.id AS student_id, s.student_no, s.name AS student_name, " +
                        "s.org_unit_id, sc.name AS class_name, " +
                        "COUNT(*) AS total, " +
                        "SUM(CASE WHEN ar.status IN (1,2) THEN 1 ELSE 0 END) AS attended, " +
                        "ROUND(SUM(CASE WHEN ar.status IN (1,2) THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) AS rate " +
                        "FROM attendance_records ar " +
                        "JOIN user_student s ON s.id = ar.student_id " +
                        "LEFT JOIN school_classes sc ON sc.id = s.org_unit_id " +
                        "WHERE ar.semester_id = ? AND s.status = 1 " +
                        orgScopeHelper.orgScopeClause("s.org_unit_id") + " " +
                        "GROUP BY s.id, s.student_no, s.name, s.org_unit_id, sc.name " +
                        "HAVING rate < ?",
                        semesterId, minRate);

                    for (Map<String, Object> student : flaggedStudents) {
                        String desc = String.format("出勤率%s%%，低于标准%d%%",
                            student.get("rate"), minRate);
                        insertWarning(semesterId, student, rule, "ATTENDANCE_LOW", warningLevel, desc, student);
                        totalWarnings++;
                    }
                    break;
                }
                case "CREDIT_SHORT": {
                    int expectedCredits = getIntParam(params, "expectedCredits", 30);
                    int actualBelow = getIntParam(params, "actualCreditsBelow", 20);
                    flaggedStudents = jdbc.queryForList(
                        "SELECT s.id AS student_id, s.student_no, s.name AS student_name, " +
                        "s.org_unit_id, sc.name AS class_name, " +
                        "COALESCE(SUM(sg.credits_earned), 0) AS earned_credits " +
                        "FROM user_student s " +
                        "LEFT JOIN school_classes sc ON sc.id = s.org_unit_id " +
                        "LEFT JOIN student_grades sg ON sg.student_id = s.id AND sg.semester_id = ? AND sg.passed = 1 " +
                        "WHERE s.status = 1 " +
                        orgScopeHelper.orgScopeClause("s.org_unit_id") + " " +
                        "GROUP BY s.id, s.student_no, s.name, s.org_unit_id, sc.name " +
                        "HAVING earned_credits < ?",
                        semesterId, actualBelow);

                    for (Map<String, Object> student : flaggedStudents) {
                        String desc = String.format("已获学分%s，低于预期%d学分",
                            student.get("earned_credits"), expectedCredits);
                        insertWarning(semesterId, student, rule, "CREDIT_SHORT", warningLevel, desc, student);
                        totalWarnings++;
                    }
                    break;
                }
                default:
                    log.warn("Unknown rule type: {}", ruleType);
            }
        }

        return Map.of("totalWarnings", totalWarnings, "rulesScanned", rules.size());
    }

    public List<Map<String, Object>> previewScan(Long semesterId) {
        List<Map<String, Object>> rules = jdbc.queryForList(
            "SELECT * FROM academic_warning_rules WHERE enabled = 1 AND deleted = 0");

        List<Map<String, Object>> previews = new ArrayList<>();

        for (Map<String, Object> rule : rules) {
            String ruleType = (String) rule.get("rule_type");
            int warningLevel = ((Number) rule.get("warning_level")).intValue();
            String paramsJson = rule.get("condition_params") != null ? rule.get("condition_params").toString() : "{}";

            Map<String, Object> params;
            try {
                params = objectMapper.readValue(paramsJson, new TypeReference<>() {});
            } catch (Exception e) {
                continue;
            }

            List<Map<String, Object>> flaggedStudents = new ArrayList<>();

            switch (ruleType) {
                case "GRADE_FAIL": {
                    int minFailCount = getIntParam(params, "minFailCount", 2);
                    flaggedStudents = jdbc.queryForList(
                        "SELECT s.id AS studentId, s.student_no AS studentNo, s.name AS studentName, " +
                        "s.org_unit_id AS orgUnitId, sc.name AS className, " +
                        "COUNT(*) AS failCount, " +
                        "GROUP_CONCAT(c.course_name SEPARATOR '、') AS failedCourses " +
                        "FROM student_grades sg " +
                        "JOIN user_student s ON s.id = sg.student_id " +
                        "LEFT JOIN school_classes sc ON sc.id = s.org_unit_id " +
                        "LEFT JOIN courses c ON c.id = sg.course_id " +
                        "WHERE sg.semester_id = ? AND sg.passed = 0 AND s.status = 1 " +
                        orgScopeHelper.orgScopeClause("s.org_unit_id") + " " +
                        "GROUP BY s.id, s.student_no, s.name, s.org_unit_id, sc.name " +
                        "HAVING failCount >= ?",
                        semesterId, minFailCount);
                    for (Map<String, Object> s : flaggedStudents) {
                        s.put("warningType", "GRADE_FAIL");
                        s.put("warningLevel", warningLevel);
                        s.put("ruleName", rule.get("rule_name"));
                        s.put("description", String.format("本学期挂科%s门: %s", s.get("failCount"), s.get("failedCourses")));
                    }
                    break;
                }
                case "ATTENDANCE_LOW": {
                    int minRate = getIntParam(params, "minAttendanceRate", 80);
                    flaggedStudents = jdbc.queryForList(
                        "SELECT s.id AS studentId, s.student_no AS studentNo, s.name AS studentName, " +
                        "s.org_unit_id AS orgUnitId, sc.name AS className, " +
                        "ROUND(SUM(CASE WHEN ar.status IN (1,2) THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) AS rate " +
                        "FROM attendance_records ar " +
                        "JOIN user_student s ON s.id = ar.student_id " +
                        "LEFT JOIN school_classes sc ON sc.id = s.org_unit_id " +
                        "WHERE ar.semester_id = ? AND s.status = 1 " +
                        orgScopeHelper.orgScopeClause("s.org_unit_id") + " " +
                        "GROUP BY s.id, s.student_no, s.name, s.org_unit_id, sc.name " +
                        "HAVING rate < ?",
                        semesterId, minRate);
                    for (Map<String, Object> s : flaggedStudents) {
                        s.put("warningType", "ATTENDANCE_LOW");
                        s.put("warningLevel", warningLevel);
                        s.put("ruleName", rule.get("rule_name"));
                        s.put("description", String.format("出勤率%s%%，低于标准%d%%", s.get("rate"), minRate));
                    }
                    break;
                }
                case "CREDIT_SHORT": {
                    int expectedCredits = getIntParam(params, "expectedCredits", 30);
                    int actualBelow = getIntParam(params, "actualCreditsBelow", 20);
                    flaggedStudents = jdbc.queryForList(
                        "SELECT s.id AS studentId, s.student_no AS studentNo, s.name AS studentName, " +
                        "s.org_unit_id AS orgUnitId, sc.name AS className, " +
                        "COALESCE(SUM(sg.credits_earned), 0) AS earnedCredits " +
                        "FROM user_student s " +
                        "LEFT JOIN school_classes sc ON sc.id = s.org_unit_id " +
                        "LEFT JOIN student_grades sg ON sg.student_id = s.id AND sg.semester_id = ? AND sg.passed = 1 " +
                        "WHERE s.status = 1 " +
                        orgScopeHelper.orgScopeClause("s.org_unit_id") + " " +
                        "GROUP BY s.id, s.student_no, s.name, s.org_unit_id, sc.name " +
                        "HAVING earnedCredits < ?",
                        semesterId, actualBelow);
                    for (Map<String, Object> s : flaggedStudents) {
                        s.put("warningType", "CREDIT_SHORT");
                        s.put("warningLevel", warningLevel);
                        s.put("ruleName", rule.get("rule_name"));
                        s.put("description", String.format("已获学分%s，低于预期%d学分", s.get("earnedCredits"), expectedCredits));
                    }
                    break;
                }
            }
            previews.addAll(flaggedStudents);
        }

        return previews;
    }

    // ==================== Warnings ====================

    public Map<String, Object> listWarnings(Integer warningLevel, Integer status, Long orgUnitId,
                                            Long studentId, String warningType, Long semesterId,
                                            int pageNum, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (warningLevel != null) { where.append(" AND warning_level = ?"); params.add(warningLevel); }
        if (status != null) { where.append(" AND status = ?"); params.add(status); }
        if (orgUnitId != null) { where.append(" AND org_unit_id = ?"); params.add(orgUnitId); }
        if (studentId != null) { where.append(" AND student_id = ?"); params.add(studentId); }
        if (warningType != null && !warningType.isEmpty()) {
            where.append(" AND warning_type = ?"); params.add(warningType);
        }
        if (semesterId != null) { where.append(" AND semester_id = ?"); params.add(semesterId); }

        // 数据权限收窄: academic_warnings 有 org_unit_id 列, JdbcTemplate 旁路 @DataPermission 拦截器
        where.append(orgScopeHelper.orgScopeClause("org_unit_id"));

        String countSql = "SELECT COUNT(*) FROM academic_warnings" + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT id, student_id AS studentId, student_no AS studentNo, student_name AS studentName, " +
            "org_unit_id AS orgUnitId, class_name AS className, rule_id AS ruleId, rule_name AS ruleName, " +
            "warning_type AS warningType, warning_level AS warningLevel, description, detail, " +
            "status, handler_id AS handlerId, handle_note AS handleNote, handled_at AS handledAt, " +
            "semester_id AS semesterId, created_at AS createdAt " +
            "FROM academic_warnings" + where + " ORDER BY warning_level DESC, status ASC, created_at DESC LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);
        List<Map<String, Object>> records = jdbc.queryForList(sql, params.toArray());

        for (Map<String, Object> record : records) {
            parseJsonField(record, "detail");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    public Map<String, Object> getWarningDetail(Long id) {
        Map<String, Object> warning = jdbc.queryForMap(
            "SELECT id, student_id AS studentId, student_no AS studentNo, student_name AS studentName, " +
            "org_unit_id AS orgUnitId, class_name AS className, rule_id AS ruleId, rule_name AS ruleName, " +
            "warning_type AS warningType, warning_level AS warningLevel, description, detail, " +
            "status, handler_id AS handlerId, handle_note AS handleNote, handled_at AS handledAt, " +
            "semester_id AS semesterId, created_at AS createdAt " +
            "FROM academic_warnings WHERE id = ?" + orgScopeHelper.orgScopeClause("org_unit_id"), id
        );
        parseJsonField(warning, "detail");
        return warning;
    }

    @Transactional
    public void confirmWarning(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        jdbc.update(
            "UPDATE academic_warnings SET status = 1, handler_id = ?, handled_at = ? WHERE id = ? AND status = 0"
                + orgScopeHelper.orgScopeClause("org_unit_id"),
            userId, LocalDateTime.now(), id
        );
    }

    @Transactional
    public void interveneWarning(Long id, String note) {
        Long userId = SecurityUtils.getCurrentUserId();
        jdbc.update(
            "UPDATE academic_warnings SET status = 2, handler_id = ?, handle_note = ?, handled_at = ? WHERE id = ? AND status IN (0,1)"
                + orgScopeHelper.orgScopeClause("org_unit_id"),
            userId, note, LocalDateTime.now(), id
        );
    }

    @Transactional
    public void dismissWarning(Long id, String note) {
        Long userId = SecurityUtils.getCurrentUserId();
        jdbc.update(
            "UPDATE academic_warnings SET status = 3, handler_id = ?, handle_note = ?, handled_at = ? WHERE id = ?"
                + orgScopeHelper.orgScopeClause("org_unit_id"),
            userId, note, LocalDateTime.now(), id
        );
    }

    public Map<String, Object> statistics(Long semesterId) {
        // 数据权限收窄: academic_warnings 有 org_unit_id 列, JdbcTemplate 旁路 @DataPermission 拦截器.
        // 用 WHERE 1=1 锚定, 让 semester 过滤与 org scope 子句都能以 " AND ..." 形式拼接.
        String scopeClause = orgScopeHelper.orgScopeClause("org_unit_id");
        String semesterWhere = " WHERE 1=1"
            + (semesterId != null ? " AND semester_id = " + semesterId : "")
            + scopeClause;

        List<Map<String, Object>> byLevel = jdbc.queryForList(
            "SELECT warning_level AS level, COUNT(*) AS count FROM academic_warnings" + semesterWhere +
            " GROUP BY warning_level ORDER BY warning_level"
        );

        List<Map<String, Object>> byType = jdbc.queryForList(
            "SELECT warning_type AS type, COUNT(*) AS count FROM academic_warnings" + semesterWhere +
            " GROUP BY warning_type"
        );

        List<Map<String, Object>> byStatus = jdbc.queryForList(
            "SELECT status, COUNT(*) AS count FROM academic_warnings" + semesterWhere +
            " GROUP BY status ORDER BY status"
        );

        Long totalWarnings = jdbc.queryForObject(
            "SELECT COUNT(*) FROM academic_warnings" + semesterWhere, Long.class
        );

        Map<String, Object> result = new HashMap<>();
        result.put("totalWarnings", totalWarnings);
        result.put("byLevel", byLevel);
        result.put("byType", byType);
        result.put("byStatus", byStatus);
        return result;
    }

    public List<Map<String, Object>> studentWarningHistory(Long studentId) {
        return jdbc.queryForList(
            "SELECT id, warning_type AS warningType, warning_level AS warningLevel, " +
            "description, status, handle_note AS handleNote, handled_at AS handledAt, " +
            "rule_name AS ruleName, semester_id AS semesterId, created_at AS createdAt " +
            "FROM academic_warnings WHERE student_id = ?"
                + orgScopeHelper.orgScopeClause("org_unit_id")
                + " ORDER BY created_at DESC",
            studentId
        );
    }

    // ==================== Helpers ====================

    private void insertWarning(Long semesterId, Map<String, Object> student,
            Map<String, Object> rule, String warningType, int warningLevel,
            String description, Map<String, Object> detail) {
        Integer existing = jdbc.queryForObject(
            "SELECT COUNT(*) FROM academic_warnings WHERE student_id = ? AND rule_id = ? AND semester_id = ? AND status != 3",
            Integer.class, student.get("student_id"), rule.get("id"), semesterId);

        if (existing == null || existing == 0) {
            String detailJson = "{}";
            try {
                detailJson = objectMapper.writeValueAsString(detail);
            } catch (Exception e) {
                log.warn("Failed to serialize detail: {}", e.getMessage());
            }

            jdbc.update(
                "INSERT INTO academic_warnings (student_id, student_no, student_name, org_unit_id, class_name, " +
                "rule_id, rule_name, warning_type, warning_level, description, detail, status, semester_id) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,0,?)",
                student.get("student_id"), student.get("student_no"), student.get("student_name"),
                student.get("org_unit_id"), student.get("class_name"),
                rule.get("id"), rule.get("rule_name"),
                warningType, warningLevel, description, detailJson, semesterId
            );
        }
    }

    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void parseJsonField(Map<String, Object> map, String fieldName) {
        Object value = map.get(fieldName);
        if (value instanceof String) {
            try {
                map.put(fieldName, objectMapper.readValue((String) value, Object.class));
            } catch (Exception e) {
                // leave as string
            }
        }
    }
}
