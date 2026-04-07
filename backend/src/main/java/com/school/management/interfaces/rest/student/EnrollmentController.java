package com.school.management.interfaces.rest.student;

import com.school.management.common.audit.Audited;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.school.management.application.event.TriggerService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 招生管理 REST Controller
 *
 * 使用 JdbcTemplate 直接操作:
 * - enrollment_plans        招生计划
 * - enrollment_applications 报名记录
 */
@Slf4j
@RestController
@RequestMapping("/enrollment")
@RequiredArgsConstructor
@Tag(name = "Enrollment", description = "招生管理API")
public class EnrollmentController {

    private final JdbcTemplate jdbc;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private TriggerService triggerService;

    // ==================== 招生计划 CRUD ====================

    @Operation(summary = "招生计划列表")
    @GetMapping("/plans")
    @CasbinAccess(resource = "enrollment", action = "view")
    public Result<Map<String, Object>> listPlans(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

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

        // Count
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
        return Result.success(result);
    }

    @Operation(summary = "创建招生计划")
    @PostMapping("/plans")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Map<String, Object>> createPlan(@RequestBody Map<String, Object> body) {
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
        return Result.success(Map.of("created", 1));
    }

    @Operation(summary = "更新招生计划")
    @PutMapping("/plans/{id}")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Void> updatePlan(@PathVariable Long id, @RequestBody Map<String, Object> body) {
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
        return Result.success();
    }

    @Operation(summary = "删除招生计划")
    @DeleteMapping("/plans/{id}")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Void> deletePlan(@PathVariable Long id) {
        jdbc.update("UPDATE enrollment_plans SET deleted=1, updated_at=NOW() WHERE id=?", id);
        return Result.success();
    }

    @Operation(summary = "发布招生计划")
    @PostMapping("/plans/{id}/publish")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Void> publishPlan(@PathVariable Long id) {
        jdbc.update(
            "UPDATE enrollment_plans SET status=1, updated_at=NOW() WHERE id=? AND status=0 AND deleted=0",
            id
        );
        return Result.success();
    }

    @Operation(summary = "招生统计")
    @GetMapping("/plans/statistics")
    @CasbinAccess(resource = "enrollment", action = "view")
    public Result<Map<String, Object>> planStatistics(
            @RequestParam(required = false) Integer year) {

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

        // Per-major breakdown
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
        return Result.success(result);
    }

    // ==================== 报名管理 CRUD ====================

    @Operation(summary = "报名列表")
    @GetMapping("/applications")
    @CasbinAccess(resource = "enrollment", action = "view")
    public Result<Map<String, Object>> listApplications(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

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

        // Count
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
        return Result.success(result);
    }

    @Operation(summary = "新增报名记录")
    @PostMapping("/applications")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Map<String, Object>> createApplication(@RequestBody Map<String, Object> body) {
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

        // If academicYear not provided, derive from plan
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
        return Result.success(Map.of("created", 1));
    }

    @Operation(summary = "更新报名记录")
    @PutMapping("/applications/{id}")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Void> updateApplication(@PathVariable Long id, @RequestBody Map<String, Object> body) {
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
        return Result.success();
    }

    @Operation(summary = "删除报名记录")
    @DeleteMapping("/applications/{id}")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Void> deleteApplication(@PathVariable Long id) {
        jdbc.update("UPDATE enrollment_applications SET deleted=1, updated_at=NOW() WHERE id=?", id);
        return Result.success();
    }

    @Operation(summary = "录取")
    @PostMapping("/applications/{id}/admit")
    @CasbinAccess(resource = "enrollment", action = "edit")
    @Audited(module = "enrollment", action = "ADMIT", resourceType = "Application", description = "录取报名")
    @Transactional
    public Result<Void> admitApplication(@PathVariable Long id) {
        Long reviewerId = SecurityUtils.getCurrentUserId();
        jdbc.update(
            "UPDATE enrollment_applications SET status=1, reviewer_id=?, reviewed_at=NOW(), " +
            "updated_at=NOW() WHERE id=? AND status=0 AND deleted=0",
            reviewerId, id
        );
        // Update plan actual_count
        updatePlanCounts(id);

        // 触发事件
        if (triggerService != null) {
            try {
                String applicantName = "";
                try {
                    applicantName = jdbc.queryForObject(
                        "SELECT applicant_name FROM enrollment_applications WHERE id=?", String.class, id);
                } catch (Exception ignored) {}
                triggerService.fire("ENROLLMENT_ADMITTED", Map.of(
                    "applicationId", id,
                    "applicantName", applicantName != null ? applicantName : ""
                ));
            } catch (Exception ignored) {}
        }

        return Result.success();
    }

    @Operation(summary = "不录取")
    @PostMapping("/applications/{id}/reject")
    @CasbinAccess(resource = "enrollment", action = "edit")
    @Audited(module = "enrollment", action = "REJECT", resourceType = "Application", description = "拒绝报名")
    public Result<Void> rejectApplication(@PathVariable Long id,
                                           @RequestBody(required = false) Map<String, Object> body) {
        Long reviewerId = SecurityUtils.getCurrentUserId();
        String comment = body != null ? (String) body.get("comment") : null;
        jdbc.update(
            "UPDATE enrollment_applications SET status=2, reviewer_id=?, reviewed_at=NOW(), " +
            "review_comment=?, updated_at=NOW() WHERE id=? AND status=0 AND deleted=0",
            reviewerId, comment, id
        );
        return Result.success();
    }

    @Operation(summary = "报到注册（创建学生记录+分配班级）")
    @PostMapping("/applications/{id}/register")
    @CasbinAccess(resource = "enrollment", action = "edit")
    @Audited(module = "enrollment", action = "REGISTER", resourceType = "Application", description = "报到注册")
    @Transactional
    public Result<Map<String, Object>> registerApplication(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long orgUnitId = toLong(body.get("orgUnitId"));
        if (orgUnitId == null) {
            return Result.error("请选择分配班级");
        }

        // 1. Get application info
        Map<String, Object> app;
        try {
            app = jdbc.queryForMap(
                "SELECT id, plan_id AS planId, applicant_name AS applicantName, gender, " +
                "id_card AS idCard, phone, status, academic_year AS academicYear " +
                "FROM enrollment_applications WHERE id=? AND deleted=0", id);
        } catch (Exception e) {
            return Result.error("报名记录不存在");
        }

        int appStatus = ((Number) app.get("status")).intValue();
        if (appStatus != 1) {
            return Result.error("只有已录取状态才能报到注册");
        }

        // 2. Generate student_no: year + 6-digit sequence
        Integer academicYear = (Integer) app.get("academicYear");
        String yearPrefix = String.valueOf(academicYear != null ? academicYear : LocalDate.now().getYear());
        Integer maxSeq;
        try {
            maxSeq = jdbc.queryForObject(
                "SELECT MAX(CAST(SUBSTRING(student_no, " + (yearPrefix.length() + 1) + ") AS UNSIGNED)) " +
                "FROM students WHERE student_no LIKE ? AND deleted=0",
                Integer.class, yearPrefix + "%");
        } catch (Exception e) {
            maxSeq = null;
        }
        int nextSeq = (maxSeq != null ? maxSeq : 0) + 1;
        String studentNo = yearPrefix + String.format("%06d", nextSeq);

        // 3. Insert student record
        Long createdBy = SecurityUtils.getCurrentUserId();
        jdbc.update(
            "INSERT INTO students (student_no, name, gender, id_card, phone, org_unit_id, " +
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

        // Get the new student ID
        Long studentId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 4. Update application: status=3(已报到), assigned_org_unit_id, assigned_student_id
        jdbc.update(
            "UPDATE enrollment_applications SET status=3, assigned_org_unit_id=?, " +
            "assigned_student_id=?, registered_at=NOW(), updated_at=NOW() WHERE id=?",
            orgUnitId, studentId, id
        );

        // 5. Update plan registered_count
        updatePlanCounts(id);

        // 触发事件
        if (triggerService != null) {
            try {
                String className = "";
                try {
                    className = jdbc.queryForObject(
                        "SELECT name FROM school_classes WHERE id=?", String.class, orgUnitId);
                } catch (Exception ignored) {}
                triggerService.fire("ENROLLMENT_REGISTERED", Map.of(
                    "studentId", studentId,
                    "studentName", app.get("applicantName") != null ? app.get("applicantName") : "",
                    "className", className != null ? className : "",
                    "applicationId", id
                ));
            } catch (Exception ignored) {}
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("studentNo", studentNo);
        return Result.success(result);
    }

    @Operation(summary = "批量录取")
    @PostMapping("/applications/batch-admit")
    @CasbinAccess(resource = "enrollment", action = "edit")
    @Transactional
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> batchAdmit(@RequestBody Map<String, Object> body) {
        List<Number> ids = (List<Number>) body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要录取的报名记录");
        }

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
        return Result.success(Map.of("admitted", count));
    }

    @Operation(summary = "导出报名记录Excel")
    @GetMapping("/applications/export")
    @CasbinAccess(resource = "enrollment", action = "view")
    public void exportApplications(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {

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

        List<Map<String, Object>> records = jdbc.queryForList(sql.toString(), params.toArray());

        String[] statusNames = {"待审核", "已录取", "未录取", "已报到", "已放弃"};

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("报名记录");

            CellStyle headerStyle = wb.createCellStyle();
            Font hf = wb.createFont();
            hf.setBold(true);
            headerStyle.setFont(hf);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle cellStyle = wb.createCellStyle();
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);
            cellStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] headers = {"年份", "姓名", "性别", "身份证号", "电话", "监护人", "监护人电话",
                "毕业学校", "报考专业", "专业方向", "报名日期", "考试成绩", "状态", "分配班级", "审核意见"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Map<String, Object> r : records) {
                Row row = sheet.createRow(rowIdx++);
                int st = r.get("status") != null ? ((Number) r.get("status")).intValue() : 0;
                Integer g = r.get("gender") != null ? ((Number) r.get("gender")).intValue() : null;
                String[] values = {
                    str(r.get("academic_year")),
                    str(r.get("applicant_name")),
                    g != null ? (g == 1 ? "男" : "女") : "",
                    str(r.get("id_card")),
                    str(r.get("phone")),
                    str(r.get("guardian_name")),
                    str(r.get("guardian_phone")),
                    str(r.get("graduate_from")),
                    str(r.get("major_name")),
                    str(r.get("direction_name")),
                    str(r.get("application_date")),
                    str(r.get("exam_score")),
                    st >= 0 && st < statusNames.length ? statusNames[st] : "",
                    str(r.get("class_name")),
                    str(r.get("review_comment"))
                };
                for (int i = 0; i < values.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(values[i]);
                    cell.setCellStyle(cellStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=enrollment_applications.xlsx");
            wb.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    // ==================== Private Helpers ====================

    /**
     * Recalculate plan actual_count and registered_count based on application statuses.
     */
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

    private String str(Object val) {
        return val != null ? val.toString() : "";
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
