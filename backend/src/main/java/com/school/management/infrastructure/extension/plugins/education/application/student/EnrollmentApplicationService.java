package com.school.management.infrastructure.extension.plugins.education.application.student;

import com.school.management.application.event.TriggerService;
import com.school.management.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.school.management.infrastructure.extension.plugins.education.constants.EducationTriggerPoints.ENROLLMENT_ADMITTED;
import static com.school.management.infrastructure.extension.plugins.education.constants.EducationTriggerPoints.ENROLLMENT_REGISTERED;

/**
 * 招生管理应用服务 (M3.2.3, 2026-05-20).
 * EnrollmentController 27 处直 jdbc 下沉.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentApplicationService {

    private final JdbcTemplate jdbc;

    @Autowired(required = false)
    private TriggerService triggerService;

    // ==================== Plans ====================

    public Map<String, Object> listPlans(Integer year, Long majorId, Integer status, Integer page, Integer size) {
        StringBuilder sql = new StringBuilder(
            "SELECT ep.id, ep.academic_year AS academicYear, ep.major_id AS majorId, " +
            "ep.major_direction_id AS majorDirectionId, ep.org_unit_id AS orgUnitId, " +
            "ep.planned_count AS plannedCount, ep.actual_count AS actualCount, " +
            "ep.registered_count AS registeredCount, ep.enrollment_target AS enrollmentTarget, " +
            "ep.status, ep.remark, ep.created_at AS createdAt, " +
            "m.name AS majorName, md.name AS majorDirectionName, ou.name AS orgUnitName " +
            "FROM enrollment_plans ep " +
            "LEFT JOIN majors m ON ep.major_id = m.id " +
            "LEFT JOIN major_directions md ON ep.major_direction_id = md.id " +
            "LEFT JOIN org_units ou ON ep.org_unit_id = ou.id " +
            "WHERE ep.deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        if (year != null) { sql.append(" AND ep.academic_year = ?"); params.add(year); }
        if (majorId != null) { sql.append(" AND ep.major_id = ?"); params.add(majorId); }
        if (status != null) { sql.append(" AND ep.status = ?"); params.add(status); }

        String countSql = "SELECT COUNT(*) FROM enrollment_plans ep WHERE ep.deleted = 0";
        StringBuilder countWhere = new StringBuilder();
        List<Object> countParams = new ArrayList<>();
        if (year != null) { countWhere.append(" AND ep.academic_year = ?"); countParams.add(year); }
        if (majorId != null) { countWhere.append(" AND ep.major_id = ?"); countParams.add(majorId); }
        if (status != null) { countWhere.append(" AND ep.status = ?"); countParams.add(status); }
        Integer total = jdbc.queryForObject(countSql + countWhere, Integer.class, countParams.toArray());

        sql.append(" ORDER BY ep.academic_year DESC, ep.created_at DESC");
        int offset = (page - 1) * size;
        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", rows);
        result.put("total", total != null ? total : 0);
        return result;
    }

    @Transactional
    public void createPlan(Map<String, Object> body) {
        Integer academicYear = toInt(body.get("academicYear"));
        Long majorId = toLong(body.get("majorId"));
        Long majorDirectionId = toLong(body.get("majorDirectionId"));
        Long orgUnitId = toLong(body.get("orgUnitId"));
        Integer plannedCount = toInt(body.get("plannedCount"));
        String enrollmentTarget = (String) body.get("enrollmentTarget");
        String remark = (String) body.get("remark");
        Long createdBy = SecurityUtils.getCurrentUserId();

        jdbc.update(
            "INSERT INTO enrollment_plans (academic_year, major_id, major_direction_id, org_unit_id, " +
            "planned_count, enrollment_target, remark, created_by) VALUES (?,?,?,?,?,?,?,?)",
            academicYear, majorId, majorDirectionId, orgUnitId,
            plannedCount, enrollmentTarget, remark, createdBy
        );
    }

    @Transactional
    public void updatePlan(Long id, Map<String, Object> body) {
        Integer academicYear = toInt(body.get("academicYear"));
        Long majorId = toLong(body.get("majorId"));
        Long majorDirectionId = toLong(body.get("majorDirectionId"));
        Long orgUnitId = toLong(body.get("orgUnitId"));
        Integer plannedCount = toInt(body.get("plannedCount"));
        String enrollmentTarget = (String) body.get("enrollmentTarget");
        String remark = (String) body.get("remark");

        jdbc.update(
            "UPDATE enrollment_plans SET academic_year=?, major_id=?, major_direction_id=?, " +
            "org_unit_id=?, planned_count=?, enrollment_target=?, remark=?, updated_at=NOW() " +
            "WHERE id=? AND deleted=0",
            academicYear, majorId, majorDirectionId, orgUnitId,
            plannedCount, enrollmentTarget, remark, id
        );
    }

    @Transactional
    public void deletePlan(Long id) {
        jdbc.update("UPDATE enrollment_plans SET deleted=1, updated_at=NOW() WHERE id=?", id);
    }

    @Transactional
    public void publishPlan(Long id) {
        jdbc.update(
            "UPDATE enrollment_plans SET status=1, updated_at=NOW() WHERE id=? AND status=0 AND deleted=0",
            id
        );
    }

    public Map<String, Object> planStatistics(Integer year) {
        StringBuilder sql = new StringBuilder(
            "SELECT COALESCE(SUM(planned_count),0) AS totalPlanned, " +
            "COALESCE(SUM(actual_count),0) AS totalAdmitted, " +
            "COALESCE(SUM(registered_count),0) AS totalRegistered, " +
            "COUNT(*) AS planCount " +
            "FROM enrollment_plans WHERE deleted=0"
        );
        List<Object> params = new ArrayList<>();
        if (year != null) { sql.append(" AND academic_year=?"); params.add(year); }

        Map<String, Object> totals = jdbc.queryForMap(sql.toString(), params.toArray());

        StringBuilder majorSql = new StringBuilder(
            "SELECT m.name AS majorName, ep.major_id AS majorId, " +
            "SUM(ep.planned_count) AS plannedCount, " +
            "SUM(ep.actual_count) AS actualCount, " +
            "SUM(ep.registered_count) AS registeredCount " +
            "FROM enrollment_plans ep " +
            "LEFT JOIN majors m ON ep.major_id = m.id " +
            "WHERE ep.deleted=0"
        );
        List<Object> majorParams = new ArrayList<>();
        if (year != null) { majorSql.append(" AND ep.academic_year=?"); majorParams.add(year); }
        majorSql.append(" GROUP BY ep.major_id, m.name ORDER BY m.name");

        List<Map<String, Object>> byMajor = jdbc.queryForList(majorSql.toString(), majorParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPlanned", totals.get("totalPlanned"));
        result.put("totalAdmitted", totals.get("totalAdmitted"));
        result.put("totalRegistered", totals.get("totalRegistered"));
        result.put("planCount", totals.get("planCount"));
        long planned = ((Number) totals.get("totalPlanned")).longValue();
        long registered = ((Number) totals.get("totalRegistered")).longValue();
        result.put("completionRate", planned > 0 ? Math.round(registered * 1000.0 / planned) / 10.0 : 0);
        result.put("byMajor", byMajor);
        return result;
    }

    // ==================== Applications ====================

    public Map<String, Object> listApplications(Integer year, Long majorId, Long planId, Integer status,
                                                String keyword, Integer page, Integer size) {
        StringBuilder sql = new StringBuilder(
            "SELECT ea.id, ea.plan_id AS planId, ea.academic_year AS academicYear, " +
            "ea.applicant_name AS applicantName, ea.gender, ea.id_card AS idCard, " +
            "ea.phone, ea.guardian_name AS guardianName, ea.guardian_phone AS guardianPhone, " +
            "ea.graduate_from AS graduateFrom, ea.major_id AS majorId, " +
            "ea.major_direction_id AS majorDirectionId, ea.application_date AS applicationDate, " +
            "ea.exam_score AS examScore, ea.status, ea.review_comment AS reviewComment, " +
            "ea.assigned_org_unit_id AS assignedClassId, ea.assigned_student_id AS assignedStudentId, " +
            "ea.remark, ea.created_at AS createdAt, ea.registered_at AS registeredAt, " +
            "m.name AS majorName, md.name AS majorDirectionName, " +
            "sc.name AS assignedClassName " +
            "FROM enrollment_applications ea " +
            "LEFT JOIN majors m ON ea.major_id = m.id " +
            "LEFT JOIN major_directions md ON ea.major_direction_id = md.id " +
            "LEFT JOIN school_classes sc ON ea.assigned_org_unit_id = sc.id " +
            "WHERE ea.deleted = 0"
        );
        List<Object> params = new ArrayList<>();

        if (year != null) { sql.append(" AND ea.academic_year = ?"); params.add(year); }
        if (majorId != null) { sql.append(" AND ea.major_id = ?"); params.add(majorId); }
        if (planId != null) { sql.append(" AND ea.plan_id = ?"); params.add(planId); }
        if (status != null) { sql.append(" AND ea.status = ?"); params.add(status); }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (ea.applicant_name LIKE ? OR ea.id_card LIKE ? OR ea.phone LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw); params.add(kw); params.add(kw);
        }

        StringBuilder countSql = new StringBuilder(
            "SELECT COUNT(*) FROM enrollment_applications ea WHERE ea.deleted = 0");
        List<Object> countParams = new ArrayList<>();
        if (year != null) { countSql.append(" AND ea.academic_year = ?"); countParams.add(year); }
        if (majorId != null) { countSql.append(" AND ea.major_id = ?"); countParams.add(majorId); }
        if (planId != null) { countSql.append(" AND ea.plan_id = ?"); countParams.add(planId); }
        if (status != null) { countSql.append(" AND ea.status = ?"); countParams.add(status); }
        if (keyword != null && !keyword.isBlank()) {
            countSql.append(" AND (ea.applicant_name LIKE ? OR ea.id_card LIKE ? OR ea.phone LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            countParams.add(kw); countParams.add(kw); countParams.add(kw);
        }
        Integer total = jdbc.queryForObject(countSql.toString(), Integer.class, countParams.toArray());

        sql.append(" ORDER BY ea.created_at DESC");
        int offset = (page - 1) * size;
        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", rows);
        result.put("total", total != null ? total : 0);
        return result;
    }

    @Transactional
    public void createApplication(Map<String, Object> body) {
        Long planId = toLong(body.get("planId"));
        Integer academicYear = toInt(body.get("academicYear"));
        String applicantName = (String) body.get("applicantName");
        Integer gender = toInt(body.get("gender"));
        String idCard = (String) body.get("idCard");
        String phone = (String) body.get("phone");
        String guardianName = (String) body.get("guardianName");
        String guardianPhone = (String) body.get("guardianPhone");
        String graduateFrom = (String) body.get("graduateFrom");
        Long majorId = toLong(body.get("majorId"));
        Long majorDirectionId = toLong(body.get("majorDirectionId"));
        String applicationDate = (String) body.get("applicationDate");
        Object examScore = body.get("examScore");
        String remark = (String) body.get("remark");

        if (academicYear == null && planId != null) {
            try {
                academicYear = jdbc.queryForObject(
                    "SELECT academic_year FROM enrollment_plans WHERE id=? AND deleted=0", Integer.class, planId);
            } catch (Exception e) {
                // ignore
            }
        }

        jdbc.update(
            "INSERT INTO enrollment_applications (plan_id, academic_year, applicant_name, gender, " +
            "id_card, phone, guardian_name, guardian_phone, graduate_from, major_id, major_direction_id, " +
            "application_date, exam_score, remark) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            planId, academicYear, applicantName, gender, idCard, phone,
            guardianName, guardianPhone, graduateFrom, majorId, majorDirectionId,
            applicationDate, examScore, remark
        );
    }

    @Transactional
    public void updateApplication(Long id, Map<String, Object> body) {
        String applicantName = (String) body.get("applicantName");
        Integer gender = toInt(body.get("gender"));
        String idCard = (String) body.get("idCard");
        String phone = (String) body.get("phone");
        String guardianName = (String) body.get("guardianName");
        String guardianPhone = (String) body.get("guardianPhone");
        String graduateFrom = (String) body.get("graduateFrom");
        Long majorId = toLong(body.get("majorId"));
        Long majorDirectionId = toLong(body.get("majorDirectionId"));
        String applicationDate = (String) body.get("applicationDate");
        Object examScore = body.get("examScore");
        String remark = (String) body.get("remark");

        jdbc.update(
            "UPDATE enrollment_applications SET applicant_name=?, gender=?, id_card=?, phone=?, " +
            "guardian_name=?, guardian_phone=?, graduate_from=?, major_id=?, major_direction_id=?, " +
            "application_date=?, exam_score=?, remark=?, updated_at=NOW() " +
            "WHERE id=? AND deleted=0",
            applicantName, gender, idCard, phone, guardianName, guardianPhone,
            graduateFrom, majorId, majorDirectionId, applicationDate, examScore, remark, id
        );
    }

    @Transactional
    public void deleteApplication(Long id) {
        jdbc.update("UPDATE enrollment_applications SET deleted=1, updated_at=NOW() WHERE id=?", id);
    }

    @Transactional
    public void admitApplication(Long id) {
        Long reviewerId = SecurityUtils.getCurrentUserId();
        jdbc.update(
            "UPDATE enrollment_applications SET status=1, reviewer_id=?, reviewed_at=NOW(), " +
            "updated_at=NOW() WHERE id=? AND status=0 AND deleted=0",
            reviewerId, id
        );
        updatePlanCounts(id);

        if (triggerService != null) {
            try {
                String applicantName = "";
                try {
                    applicantName = jdbc.queryForObject(
                        "SELECT applicant_name FROM enrollment_applications WHERE id=?", String.class, id);
                } catch (Exception ignored) {}
                triggerService.fire(ENROLLMENT_ADMITTED, Map.of(
                    "applicationId", id,
                    "applicantName", applicantName != null ? applicantName : ""
                ));
            } catch (Exception ignored) {}
        }
    }

    @Transactional
    public void rejectApplication(Long id, String comment) {
        Long reviewerId = SecurityUtils.getCurrentUserId();
        jdbc.update(
            "UPDATE enrollment_applications SET status=2, reviewer_id=?, reviewed_at=NOW(), " +
            "review_comment=?, updated_at=NOW() WHERE id=? AND status=0 AND deleted=0",
            reviewerId, comment, id
        );
    }

    /**
     * Returns map with: error (String, null if success), studentId, studentNo.
     */
    @Transactional
    public Map<String, Object> registerApplication(Long id, Long orgUnitId) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null) {
            out.put("error", "请选择分配班级");
            return out;
        }

        Map<String, Object> app;
        try {
            app = jdbc.queryForMap(
                "SELECT id, plan_id AS planId, applicant_name AS applicantName, gender, " +
                "id_card AS idCard, phone, status, academic_year AS academicYear " +
                "FROM enrollment_applications WHERE id=? AND deleted=0", id);
        } catch (Exception e) {
            out.put("error", "报名记录不存在");
            return out;
        }

        int appStatus = ((Number) app.get("status")).intValue();
        if (appStatus != 1) {
            out.put("error", "只有已录取状态才能报到注册");
            return out;
        }

        Integer academicYear = (Integer) app.get("academicYear");
        String yearPrefix = String.valueOf(academicYear != null ? academicYear : LocalDate.now().getYear());
        Integer maxSeq;
        try {
            maxSeq = jdbc.queryForObject(
                "SELECT MAX(CAST(SUBSTRING(student_no, " + (yearPrefix.length() + 1) + ") AS UNSIGNED)) " +
                "FROM user_student WHERE student_no LIKE ? AND deleted=0",
                Integer.class, yearPrefix + "%");
        } catch (Exception e) {
            maxSeq = null;
        }
        int nextSeq = (maxSeq != null ? maxSeq : 0) + 1;
        String studentNo = yearPrefix + String.format("%06d", nextSeq);

        Long createdBy = SecurityUtils.getCurrentUserId();
        jdbc.update(
            "INSERT INTO user_student (student_no, name, gender, id_card, phone, org_unit_id, " +
            "enrollment_date, status, created_by, created_at) " +
            "VALUES (?,?,?,?,?,?,?,1,?,NOW())",
            studentNo,
            app.get("applicantName"),
            app.get("gender"),
            app.get("idCard"),
            app.get("phone"),
            orgUnitId,
            LocalDate.now(),
            createdBy
        );

        Long studentId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        jdbc.update(
            "UPDATE enrollment_applications SET status=3, assigned_org_unit_id=?, " +
            "assigned_student_id=?, registered_at=NOW(), updated_at=NOW() WHERE id=?",
            orgUnitId, studentId, id
        );

        updatePlanCounts(id);

        if (triggerService != null) {
            try {
                String className = "";
                try {
                    className = jdbc.queryForObject(
                        "SELECT name FROM school_classes WHERE id=?", String.class, orgUnitId);
                } catch (Exception ignored) {}
                triggerService.fire(ENROLLMENT_REGISTERED, Map.of(
                    "studentId", studentId,
                    "studentName", app.get("applicantName") != null ? app.get("applicantName") : "",
                    "className", className != null ? className : "",
                    "applicationId", id
                ));
            } catch (Exception ignored) {}
        }

        out.put("error", null);
        out.put("studentId", studentId);
        out.put("studentNo", studentNo);
        return out;
    }

    @Transactional
    public int batchAdmit(List<Number> ids) {
        Long reviewerId = SecurityUtils.getCurrentUserId();
        int count = 0;
        for (Number idNum : ids) {
            Long appId = idNum.longValue();
            int updated = jdbc.update(
                "UPDATE enrollment_applications SET status=1, reviewer_id=?, reviewed_at=NOW(), " +
                "updated_at=NOW() WHERE id=? AND status=0 AND deleted=0",
                reviewerId, appId
            );
            if (updated > 0) {
                updatePlanCounts(appId);
                count++;
            }
        }
        return count;
    }

    public List<Map<String, Object>> exportApplicationsRaw(Integer year, Long majorId, Integer status) {
        StringBuilder sql = new StringBuilder(
            "SELECT ea.applicant_name, ea.gender, ea.id_card, ea.phone, " +
            "ea.guardian_name, ea.guardian_phone, ea.graduate_from, " +
            "ea.application_date, ea.exam_score, ea.status, ea.review_comment, " +
            "ea.academic_year, m.name AS major_name, md.name AS direction_name, " +
            "sc.name AS class_name " +
            "FROM enrollment_applications ea " +
            "LEFT JOIN majors m ON ea.major_id = m.id " +
            "LEFT JOIN major_directions md ON ea.major_direction_id = md.id " +
            "LEFT JOIN school_classes sc ON ea.assigned_org_unit_id = sc.id " +
            "WHERE ea.deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        if (year != null) { sql.append(" AND ea.academic_year = ?"); params.add(year); }
        if (majorId != null) { sql.append(" AND ea.major_id = ?"); params.add(majorId); }
        if (status != null) { sql.append(" AND ea.status = ?"); params.add(status); }
        sql.append(" ORDER BY ea.academic_year DESC, ea.created_at DESC");

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    private void updatePlanCounts(Long applicationId) {
        try {
            Long planId = jdbc.queryForObject(
                "SELECT plan_id FROM enrollment_applications WHERE id=?", Long.class, applicationId);
            if (planId != null) {
                jdbc.update(
                    "UPDATE enrollment_plans SET " +
                    "actual_count = (SELECT COUNT(*) FROM enrollment_applications WHERE plan_id=? AND status IN (1,3) AND deleted=0), " +
                    "registered_count = (SELECT COUNT(*) FROM enrollment_applications WHERE plan_id=? AND status=3 AND deleted=0), " +
                    "updated_at=NOW() WHERE id=?",
                    planId, planId, planId
                );
            }
        } catch (Exception e) {
            log.warn("Failed to update plan counts for application {}: {}", applicationId, e.getMessage());
        }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.valueOf(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.valueOf(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
