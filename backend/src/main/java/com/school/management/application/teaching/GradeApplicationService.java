package com.school.management.application.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.infrastructure.persistence.teaching.grade.GradeBatchMapper;
import com.school.management.infrastructure.persistence.teaching.grade.GradeBatchPO;
import com.school.management.infrastructure.persistence.teaching.grade.StudentGradeMapper;
import com.school.management.infrastructure.persistence.teaching.grade.StudentGradePO;
import com.school.management.application.event.TriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service("teachingGradeApplicationService")
@RequiredArgsConstructor
public class GradeApplicationService {

    private final GradeBatchMapper batchMapper;
    private final StudentGradeMapper gradeMapper;
    private final JdbcTemplate jdbc; // for complex queries with dynamic filters

    @Autowired(required = false)
    private TriggerService triggerService;

    // ==================== Batch Methods ====================

    public Map<String, Object> listBatches(Long semesterId, Integer gradeType, Integer status,
                                           int pageNum, int pageSize) {
        LambdaQueryWrapper<GradeBatchPO> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null) wrapper.eq(GradeBatchPO::getSemesterId, semesterId);
        if (gradeType != null) wrapper.eq(GradeBatchPO::getGradeType, gradeType);
        if (status != null) wrapper.eq(GradeBatchPO::getStatus, status);
        wrapper.orderByDesc(GradeBatchPO::getCreatedAt);

        Page<GradeBatchPO> page = batchMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        return result;
    }

    public GradeBatchPO getBatch(Long id) {
        return batchMapper.selectById(id);
    }

    @Transactional
    public GradeBatchPO createBatch(Map<String, Object> data, Long currentUserId) {
        GradeBatchPO po = new GradeBatchPO();
        long id = IdWorker.getId();
        po.setId(id);
        po.setBatchCode("GB" + id);
        po.setBatchName((String) data.get("batchName"));
        po.setSemesterId(toLong(data.get("semesterId")));
        po.setCourseId(toLong(data.get("courseId")));
        po.setOrgUnitId(toLong(data.get("orgUnitId")));
        po.setGradeType(toInt(data.get("gradeType"), 1));
        po.setStartTime(toLocalDateTime(data.get("startTime")));
        po.setEndTime(toLocalDateTime(data.get("endTime")));
        po.setStatus(toInt(data.get("status"), 0));
        po.setCreatedBy(currentUserId);

        batchMapper.insert(po);
        return po;
    }

    @Transactional
    public void updateBatch(Long id, Map<String, Object> data) {
        GradeBatchPO po = batchMapper.selectById(id);
        if (po == null) throw new RuntimeException("成绩批次不存在: " + id);

        po.setBatchName((String) data.get("batchName"));
        po.setSemesterId(toLong(data.get("semesterId")));
        po.setCourseId(toLong(data.get("courseId")));
        po.setOrgUnitId(toLong(data.get("orgUnitId")));
        po.setGradeType(toIntOrNull(data.get("gradeType")));
        po.setStartTime(toLocalDateTime(data.get("startTime")));
        po.setEndTime(toLocalDateTime(data.get("endTime")));
        po.setStatus(toIntOrNull(data.get("status")));

        batchMapper.updateById(po);
    }

    @Transactional
    public void deleteBatch(Long id) {
        batchMapper.deleteById(id); // physical delete, matching original behavior
    }

    @Transactional
    public void submitBatch(Long id) {
        GradeBatchPO po = batchMapper.selectById(id);
        if (po == null) throw new RuntimeException("成绩批次不存在: " + id);
        po.setStatus(1);
        batchMapper.updateById(po);
    }

    @Transactional
    public void approveBatch(Long id) {
        GradeBatchPO po = batchMapper.selectById(id);
        if (po == null) throw new RuntimeException("成绩批次不存在: " + id);
        po.setStatus(2);
        batchMapper.updateById(po);
    }

    @Transactional
    public void publishBatch(Long id) {
        GradeBatchPO po = batchMapper.selectById(id);
        if (po == null) throw new RuntimeException("成绩批次不存在: " + id);
        po.setStatus(3);
        batchMapper.updateById(po);

        // 触发事件: 成绩发布
        if (triggerService != null) {
            try {
                triggerService.fire("GRADE_PUBLISHED", Map.of(
                    "batchId", id,
                    "batchName", po.getBatchName() != null ? po.getBatchName() : "",
                    "semesterId", po.getSemesterId() != null ? po.getSemesterId() : 0L
                ));
            } catch (Exception ignored) {}
        }
    }

    // ==================== Grade CRUD ====================

    public List<Map<String, Object>> listGrades(Long batchId) {
        try {
            return gradeMapper.listByBatchWithStudentInfo(batchId);
        } catch (Exception e) {
            log.warn("Failed to query grades with student join, falling back: {}", e.getMessage());
            LambdaQueryWrapper<StudentGradePO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StudentGradePO::getBatchId, batchId)
                   .orderByDesc(StudentGradePO::getCreatedAt);
            List<StudentGradePO> list = gradeMapper.selectList(wrapper);
            List<Map<String, Object>> result = new ArrayList<>();
            for (StudentGradePO g : list) {
                result.add(gradeToMap(g));
            }
            return result;
        }
    }

    @Transactional
    public StudentGradePO recordGrade(Long batchId, Map<String, Object> data) {
        StudentGradePO po = new StudentGradePO();
        long id = IdWorker.getId();
        po.setId(id);
        po.setBatchId(batchId);
        po.setSemesterId(toLong(data.get("semesterId")));
        po.setTaskId(toLong(data.get("taskId")));
        po.setCourseId(toLong(data.get("courseId")));
        po.setStudentId(toLong(data.get("studentId")));
        po.setOrgUnitId(toLong(data.get("orgUnitId")));
        po.setTotalScore(toBigDecimal(data.get("totalScore")));
        po.setGradePoint(toBigDecimal(data.get("gradePoint")));
        po.setPassed(toIntOrNull(data.get("passed")));
        po.setCreditsEarned(toBigDecimal(data.get("creditsEarned")));
        po.setGradeStatus(toInt(data.get("gradeStatus"), 1));
        po.setRemark((String) data.get("remark"));
        po.setDeleted(0);

        gradeMapper.insert(po);
        return po;
    }

    @Transactional
    public void updateGrade(Long gradeId, Map<String, Object> data) {
        StudentGradePO po = gradeMapper.selectById(gradeId);
        if (po == null) throw new RuntimeException("成绩记录不存在: " + gradeId);

        po.setTotalScore(toBigDecimal(data.get("totalScore")));
        po.setGradePoint(toBigDecimal(data.get("gradePoint")));
        po.setPassed(toIntOrNull(data.get("passed")));
        po.setCreditsEarned(toBigDecimal(data.get("creditsEarned")));
        po.setGradeStatus(toIntOrNull(data.get("gradeStatus")));
        po.setRemark((String) data.get("remark"));

        gradeMapper.updateById(po);
    }

    @Transactional
    public void batchRecordGrades(Long batchId, List<Map<String, Object>> grades) {
        if (grades == null || grades.isEmpty()) return;

        // Get batch info for semesterId
        Long semesterId = null;
        GradeBatchPO batch = batchMapper.selectById(batchId);
        if (batch != null) {
            semesterId = batch.getSemesterId();
        }

        for (Map<String, Object> grade : grades) {
            StudentGradePO po = new StudentGradePO();
            long id = IdWorker.getId();
            po.setId(id);
            po.setBatchId(batchId);
            po.setSemesterId(semesterId);
            po.setTaskId(toLong(grade.get("taskId")));
            po.setCourseId(toLong(grade.get("courseId")));
            po.setStudentId(toLong(grade.get("studentId")));
            po.setOrgUnitId(toLong(grade.get("orgUnitId")));
            po.setTotalScore(toBigDecimal(grade.get("totalScore")));
            po.setGradePoint(toBigDecimal(grade.get("gradePoint")));
            po.setPassed(toIntOrNull(grade.get("passed")));
            po.setCreditsEarned(toBigDecimal(grade.get("creditsEarned")));
            po.setGradeStatus(1);
            po.setRemark((String) grade.get("remark"));
            po.setDeleted(0);

            gradeMapper.insert(po);
        }
    }

    // ==================== Query Methods ====================

    public List<Map<String, Object>> getStudentGrades(Long studentId, Long semesterId, Long courseId) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        try {
            sql.append(
                "SELECT g.id, g.batch_id AS batchId, g.semester_id AS semesterId, " +
                "g.task_id AS taskId, g.course_id AS courseId, g.student_id AS studentId, " +
                "g.org_unit_id AS orgUnitId, g.total_score AS totalScore, " +
                "g.grade_point AS gradePoint, g.passed, g.credits_earned AS creditsEarned, " +
                "g.grade_status AS gradeStatus, g.remark, " +
                "c.course_name AS courseName " +
                "FROM student_grades g " +
                "LEFT JOIN courses c ON g.course_id = c.id " +
                "WHERE g.student_id = ? AND g.deleted = 0"
            );
            params.add(studentId);

            if (semesterId != null) {
                sql.append(" AND g.semester_id = ?");
                params.add(semesterId);
            }
            if (courseId != null) {
                sql.append(" AND g.course_id = ?");
                params.add(courseId);
            }
            sql.append(" ORDER BY g.created_at DESC");

            return jdbc.queryForList(sql.toString(), params.toArray());
        } catch (Exception e) {
            log.warn("Failed to query student grades with join, falling back: {}", e.getMessage());
            sql.setLength(0);
            params.clear();

            sql.append(
                "SELECT id, batch_id AS batchId, semester_id AS semesterId, " +
                "task_id AS taskId, course_id AS courseId, student_id AS studentId, " +
                "org_unit_id AS orgUnitId, total_score AS totalScore, " +
                "grade_point AS gradePoint, passed, credits_earned AS creditsEarned, " +
                "grade_status AS gradeStatus, remark " +
                "FROM student_grades WHERE student_id = ? AND deleted = 0"
            );
            params.add(studentId);
            if (semesterId != null) {
                sql.append(" AND semester_id = ?");
                params.add(semesterId);
            }
            if (courseId != null) {
                sql.append(" AND course_id = ?");
                params.add(courseId);
            }
            sql.append(" ORDER BY created_at DESC");
            return jdbc.queryForList(sql.toString(), params.toArray());
        }
    }

    public List<Map<String, Object>> getClassGrades(Long orgUnitId) {
        try {
            return gradeMapper.listByClassWithJoins(orgUnitId);
        } catch (Exception e) {
            log.warn("Failed to query class grades with joins, falling back: {}", e.getMessage());
            LambdaQueryWrapper<StudentGradePO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StudentGradePO::getOrgUnitId, orgUnitId)
                   .orderByAsc(StudentGradePO::getCourseId)
                   .orderByDesc(StudentGradePO::getTotalScore);
            List<StudentGradePO> list = gradeMapper.selectList(wrapper);
            List<Map<String, Object>> result = new ArrayList<>();
            for (StudentGradePO g : list) {
                result.add(gradeToMap(g));
            }
            return result;
        }
    }

    // ==================== Statistics & Ranking ====================

    public Map<String, Object> getStatistics(Long batchId, Long orgUnitId, Long courseId) {
        StringBuilder where = new StringBuilder(" WHERE deleted = 0");
        List<Object> params = new ArrayList<>();

        if (batchId != null) {
            where.append(" AND batch_id = ?");
            params.add(batchId);
        }
        if (orgUnitId != null) {
            where.append(" AND org_unit_id = ?");
            params.add(orgUnitId);
        }
        if (courseId != null) {
            where.append(" AND course_id = ?");
            params.add(courseId);
        }

        String statsSql = "SELECT " +
            "COUNT(*) AS totalCount, " +
            "AVG(total_score) AS avgScore, " +
            "MAX(total_score) AS maxScore, " +
            "MIN(total_score) AS minScore, " +
            "SUM(CASE WHEN passed = 1 THEN 1 ELSE 0 END) AS passedCount " +
            "FROM student_grades" + where;
        Map<String, Object> stats = jdbc.queryForMap(statsSql, params.toArray());

        String distSql = "SELECT " +
            "SUM(CASE WHEN total_score >= 90 THEN 1 ELSE 0 END) AS excellent, " +
            "SUM(CASE WHEN total_score >= 80 AND total_score < 90 THEN 1 ELSE 0 END) AS good, " +
            "SUM(CASE WHEN total_score >= 70 AND total_score < 80 THEN 1 ELSE 0 END) AS medium, " +
            "SUM(CASE WHEN total_score >= 60 AND total_score < 70 THEN 1 ELSE 0 END) AS pass, " +
            "SUM(CASE WHEN total_score < 60 THEN 1 ELSE 0 END) AS fail " +
            "FROM student_grades" + where;
        Map<String, Object> distribution = jdbc.queryForMap(distSql, params.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", stats.get("totalCount"));
        result.put("avgScore", stats.get("avgScore"));
        result.put("maxScore", stats.get("maxScore"));
        result.put("minScore", stats.get("minScore"));
        result.put("passedCount", stats.get("passedCount"));
        result.put("distribution", distribution);
        return result;
    }

    public List<Map<String, Object>> getRanking(Long orgUnitId, Long semesterId) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        boolean joinStudents = true;

        try {
            jdbc.queryForObject("SELECT 1 FROM students LIMIT 1", Integer.class);
            sql.append(
                "SELECT g.student_id AS studentId, " +
                "s.name AS studentName, s.student_no AS studentNo, " +
                "SUM(g.total_score) AS totalScore, " +
                "AVG(g.total_score) AS avgScore, " +
                "COUNT(*) AS courseCount " +
                "FROM student_grades g " +
                "LEFT JOIN students s ON g.student_id = s.id " +
                "WHERE g.org_unit_id = ? AND g.deleted = 0"
            );
        } catch (Exception e) {
            joinStudents = false;
            sql.append(
                "SELECT student_id AS studentId, " +
                "SUM(total_score) AS totalScore, " +
                "AVG(total_score) AS avgScore, " +
                "COUNT(*) AS courseCount " +
                "FROM student_grades " +
                "WHERE org_unit_id = ? AND deleted = 0"
            );
        }
        params.add(orgUnitId);

        if (semesterId != null) {
            sql.append(joinStudents ? " AND g.semester_id = ?" : " AND semester_id = ?");
            params.add(semesterId);
        }

        sql.append(joinStudents ? " GROUP BY g.student_id, s.name, s.student_no" : " GROUP BY student_id");
        sql.append(" ORDER BY totalScore DESC");

        List<Map<String, Object>> ranking = jdbc.queryForList(sql.toString(), params.toArray());

        int rank = 1;
        for (Map<String, Object> row : ranking) {
            row.put("rank", rank++);
        }

        return ranking;
    }

    // ==================== Export ====================

    public void exportGrades(Long semesterId, Long orgUnitId, Long courseId,
                             HttpServletResponse response) throws IOException {
        StringBuilder sql = new StringBuilder(
            "SELECT sg.total_score, sg.grade_point, sg.passed, " +
            "s.student_no, s.name AS student_name, " +
            "c.course_name, sc.name AS class_name " +
            "FROM student_grades sg " +
            "LEFT JOIN students s ON s.id = sg.student_id " +
            "LEFT JOIN courses c ON c.id = sg.course_id " +
            "LEFT JOIN school_classes sc ON sc.id = sg.org_unit_id " +
            "WHERE sg.semester_id = ? AND sg.deleted = 0");
        List<Object> params = new ArrayList<>();
        params.add(semesterId);
        if (orgUnitId != null) { sql.append(" AND sg.org_unit_id = ?"); params.add(orgUnitId); }
        if (courseId != null) { sql.append(" AND sg.course_id = ?"); params.add(courseId); }
        sql.append(" ORDER BY sc.name, s.student_no");

        List<Map<String, Object>> grades = jdbc.queryForList(sql.toString(), params.toArray());

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("成绩表");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
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

            // Headers
            String[] headers = {"学号", "姓名", "班级", "课程", "成绩", "绩点", "是否通过"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (Map<String, Object> g : grades) {
                Row row = sheet.createRow(rowIdx++);
                String[] values = {
                    str(g.get("student_no")), str(g.get("student_name")),
                    str(g.get("class_name")), str(g.get("course_name")),
                    str(g.get("total_score")), str(g.get("grade_point")),
                    g.get("passed") != null && ((Number) g.get("passed")).intValue() == 1 ? "是" : "否"
                };
                for (int i = 0; i < values.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(values[i]);
                    cell.setCellStyle(cellStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=grades.xlsx");
            wb.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    // ==================== Import Template & Import ====================

    /**
     * 生成成绩导入 Excel 模板，预填该批次关联班级的学生信息
     */
    public void generateImportTemplate(Long batchId, HttpServletResponse response) throws IOException {
        GradeBatchPO batch = batchMapper.selectById(batchId);
        if (batch == null) throw new RuntimeException("成绩批次不存在: " + batchId);

        // 查询关联学生列表
        List<Map<String, Object>> students = queryStudentsForBatch(batch);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("成绩导入模板");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
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

            // Headers: A=学号, B=姓名, C=班级, D=成绩, E=备注, F=student_id(隐藏)
            String[] headers = {"学号", "姓名", "班级", "成绩", "备注", "student_id"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill student data rows
            int rowIdx = 1;
            for (Map<String, Object> stu : students) {
                Row row = sheet.createRow(rowIdx++);

                Cell c0 = row.createCell(0);
                c0.setCellValue(str(stu.get("student_no")));
                c0.setCellStyle(cellStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(str(stu.get("name")));
                c1.setCellStyle(cellStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(str(stu.get("class_name")));
                c2.setCellStyle(cellStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(""); // 成绩留空
                c3.setCellStyle(cellStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(""); // 备注留空
                c4.setCellStyle(cellStyle);

                // Hidden column F: student_id
                Cell c5 = row.createCell(5);
                c5.setCellValue(str(stu.get("id")));
                c5.setCellStyle(cellStyle);
            }

            // Auto-size visible columns
            for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);

            // Hide column F (student_id)
            sheet.setColumnHidden(5, true);

            // Response headers
            String fileName = "grade_import_template_" + batchId + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            wb.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    /**
     * 导入成绩 Excel 文件，解析并写入数据库
     */
    @Transactional
    public Map<String, Object> importGrades(Long batchId, InputStream inputStream) throws IOException {
        GradeBatchPO batch = batchMapper.selectById(batchId);
        if (batch == null) throw new RuntimeException("成绩批次不存在: " + batchId);

        int successCount = 0;
        int errorCount = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(inputStream)) {
            Sheet sheet = wb.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // Read cells
                    String studentNo = getCellStringValue(row.getCell(0));
                    String scoreStr = getCellStringValue(row.getCell(3));
                    String remark = getCellStringValue(row.getCell(4));
                    String studentIdStr = getCellStringValue(row.getCell(5));

                    // Skip empty rows (no student_no and no student_id)
                    if ((studentNo == null || studentNo.isBlank()) &&
                        (studentIdStr == null || studentIdStr.isBlank())) {
                        continue;
                    }

                    // Resolve studentId
                    Long studentId = null;
                    if (studentIdStr != null && !studentIdStr.isBlank()) {
                        try {
                            // Handle potential decimal format from Excel (e.g. "12345.0")
                            if (studentIdStr.contains(".")) {
                                studentId = new BigDecimal(studentIdStr).longValue();
                            } else {
                                studentId = Long.parseLong(studentIdStr);
                            }
                        } catch (NumberFormatException e) {
                            // fall through to studentNo lookup
                        }
                    }
                    if (studentId == null && studentNo != null && !studentNo.isBlank()) {
                        try {
                            List<Map<String, Object>> found = jdbc.queryForList(
                                "SELECT id FROM students WHERE student_no = ? AND deleted = 0 LIMIT 1",
                                studentNo.trim()
                            );
                            if (!found.isEmpty()) {
                                studentId = ((Number) found.get(0).get("id")).longValue();
                            }
                        } catch (Exception e) {
                            // ignore lookup errors
                        }
                    }
                    if (studentId == null) {
                        errorCount++;
                        errors.add(Map.of("row", i + 1, "error", "无法识别学生: 学号=" + studentNo));
                        continue;
                    }

                    // Parse and validate score
                    if (scoreStr == null || scoreStr.isBlank()) {
                        errorCount++;
                        errors.add(Map.of("row", i + 1, "error", "成绩为空"));
                        continue;
                    }
                    BigDecimal score;
                    try {
                        score = new BigDecimal(scoreStr.trim());
                    } catch (NumberFormatException e) {
                        errorCount++;
                        errors.add(Map.of("row", i + 1, "error", "成绩格式不正确: " + scoreStr));
                        continue;
                    }
                    if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
                        errorCount++;
                        errors.add(Map.of("row", i + 1, "error", "成绩超出范围(0-100): " + score));
                        continue;
                    }

                    // Calculate gradeLevel and gradePoint
                    String gradeLevel = calcGradeLevel(score);
                    BigDecimal gradePoint = calcGradePoint(score);
                    int passed = score.compareTo(new BigDecimal("60")) >= 0 ? 1 : 0;

                    // Upsert: find existing record by batchId + studentId
                    LambdaQueryWrapper<StudentGradePO> existWrapper = new LambdaQueryWrapper<>();
                    existWrapper.eq(StudentGradePO::getBatchId, batchId)
                                .eq(StudentGradePO::getStudentId, studentId);
                    StudentGradePO existing = gradeMapper.selectOne(existWrapper);

                    if (existing != null) {
                        // Update existing record
                        existing.setTotalScore(score);
                        existing.setGradeLevel(gradeLevel);
                        existing.setGradePoint(gradePoint);
                        existing.setPassed(passed);
                        existing.setRemark(remark);
                        existing.setGradeStatus(1);
                        gradeMapper.updateById(existing);
                    } else {
                        // Insert new record
                        StudentGradePO po = new StudentGradePO();
                        po.setId(IdWorker.getId());
                        po.setBatchId(batchId);
                        po.setSemesterId(batch.getSemesterId());
                        po.setCourseId(batch.getCourseId());
                        po.setStudentId(studentId);
                        po.setOrgUnitId(batch.getOrgUnitId());
                        po.setTotalScore(score);
                        po.setGradeLevel(gradeLevel);
                        po.setGradePoint(gradePoint);
                        po.setPassed(passed);
                        po.setGradeStatus(1);
                        po.setRemark(remark);
                        po.setDeleted(0);
                        gradeMapper.insert(po);
                    }
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    errors.add(Map.of("row", i + 1, "error", "处理异常: " + e.getMessage()));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        result.put("errors", errors);
        return result;
    }

    // ---- Import helpers ----

    private List<Map<String, Object>> queryStudentsForBatch(GradeBatchPO batch) {
        Long orgUnitId = batch.getOrgUnitId();
        if (orgUnitId != null) {
            // orgUnitId is class_id in students table
            try {
                return jdbc.queryForList(
                    "SELECT s.id, s.student_no, s.name, sc.name AS class_name " +
                    "FROM students s " +
                    "LEFT JOIN school_classes sc ON sc.id = s.class_id " +
                    "WHERE s.class_id = ? AND s.deleted = 0 ORDER BY s.student_no",
                    orgUnitId
                );
            } catch (Exception e) {
                log.warn("Failed to query students by orgUnitId: {}", e.getMessage());
            }
        }

        // Fallback: try courseId to find enrolled students
        Long courseId = batch.getCourseId();
        if (courseId != null) {
            try {
                return jdbc.queryForList(
                    "SELECT DISTINCT s.id, s.student_no, s.name, sc.name AS class_name " +
                    "FROM student_grades sg " +
                    "JOIN students s ON s.id = sg.student_id " +
                    "LEFT JOIN school_classes sc ON sc.id = s.class_id " +
                    "WHERE sg.course_id = ? AND sg.deleted = 0 AND s.deleted = 0 " +
                    "ORDER BY s.student_no",
                    courseId
                );
            } catch (Exception e) {
                log.warn("Failed to query students by courseId: {}", e.getMessage());
            }
        }

        // Return empty list — template will have headers only
        return Collections.emptyList();
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Avoid scientific notation for large numbers like student IDs
                double numVal = cell.getNumericCellValue();
                if (numVal == Math.floor(numVal) && !Double.isInfinite(numVal)) {
                    return String.valueOf((long) numVal);
                }
                return BigDecimal.valueOf(numVal).toPlainString();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return null;
        }
    }

    private String calcGradeLevel(BigDecimal score) {
        double s = score.doubleValue();
        if (s >= 90) return "A";
        if (s >= 85) return "B+";
        if (s >= 82) return "B";
        if (s >= 78) return "B-";
        if (s >= 75) return "C+";
        if (s >= 72) return "C";
        if (s >= 68) return "C-";
        if (s >= 64) return "D+";
        if (s >= 60) return "D";
        return "F";
    }

    private BigDecimal calcGradePoint(BigDecimal score) {
        double s = score.doubleValue();
        if (s >= 90) return new BigDecimal("4.0");
        if (s >= 85) return new BigDecimal("3.7");
        if (s >= 82) return new BigDecimal("3.3");
        if (s >= 78) return new BigDecimal("3.0");
        if (s >= 75) return new BigDecimal("2.7");
        if (s >= 72) return new BigDecimal("2.3");
        if (s >= 68) return new BigDecimal("2.0");
        if (s >= 64) return new BigDecimal("1.5");
        if (s >= 60) return new BigDecimal("1.0");
        return BigDecimal.ZERO;
    }

    // ==================== Utility Methods ====================

    private Map<String, Object> gradeToMap(StudentGradePO g) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", g.getId());
        map.put("batchId", g.getBatchId());
        map.put("semesterId", g.getSemesterId());
        map.put("taskId", g.getTaskId());
        map.put("courseId", g.getCourseId());
        map.put("studentId", g.getStudentId());
        map.put("orgUnitId", g.getOrgUnitId());
        map.put("totalScore", g.getTotalScore());
        map.put("gradePoint", g.getGradePoint());
        map.put("passed", g.getPassed());
        map.put("creditsEarned", g.getCreditsEarned());
        map.put("gradeStatus", g.getGradeStatus());
        map.put("remark", g.getRemark());
        map.put("createdAt", g.getCreatedAt());
        map.put("updatedAt", g.getUpdatedAt());
        return map;
    }

    private String str(Object val) {
        return val != null ? val.toString() : "";
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Integer toIntOrNull(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        return new BigDecimal(val.toString());
    }

    private LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDateTime) return (LocalDateTime) val;
        return LocalDateTime.parse(val.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
