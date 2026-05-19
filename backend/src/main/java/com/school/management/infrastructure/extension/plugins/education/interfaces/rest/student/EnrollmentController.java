package com.school.management.infrastructure.extension.plugins.education.interfaces.rest.student;

import com.school.management.common.audit.Audited;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.extension.plugins.education.application.student.EnrollmentApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

/**
 * 招生管理 REST Controller.
 * <p>M3.2.3 (2026-05-20): 27 处直 jdbc 已下沉到 EnrollmentApplicationService.
 */
@Slf4j
@RestController
@RequestMapping("/enrollment")
@RequiredArgsConstructor
@Tag(name = "Enrollment", description = "招生管理API")
public class EnrollmentController {

    private final EnrollmentApplicationService enrollmentService;

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
        return Result.success(enrollmentService.listPlans(year, majorId, status, page, size));
    }

    @Operation(summary = "创建招生计划")
    @PostMapping("/plans")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Map<String, Object>> createPlan(@RequestBody Map<String, Object> body) {
        enrollmentService.createPlan(body);
        return Result.success(Map.of("created", 1));
    }

    @Operation(summary = "更新招生计划")
    @PutMapping("/plans/{id}")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Void> updatePlan(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        enrollmentService.updatePlan(id, body);
        return Result.success();
    }

    @Operation(summary = "删除招生计划")
    @DeleteMapping("/plans/{id}")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Void> deletePlan(@PathVariable Long id) {
        enrollmentService.deletePlan(id);
        return Result.success();
    }

    @Operation(summary = "发布招生计划")
    @PostMapping("/plans/{id}/publish")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Void> publishPlan(@PathVariable Long id) {
        enrollmentService.publishPlan(id);
        return Result.success();
    }

    @Operation(summary = "招生统计")
    @GetMapping("/plans/statistics")
    @CasbinAccess(resource = "enrollment", action = "view")
    public Result<Map<String, Object>> planStatistics(@RequestParam(required = false) Integer year) {
        return Result.success(enrollmentService.planStatistics(year));
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
        return Result.success(enrollmentService.listApplications(year, majorId, planId, status, keyword, page, size));
    }

    @Operation(summary = "新增报名记录")
    @PostMapping("/applications")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Map<String, Object>> createApplication(@RequestBody Map<String, Object> body) {
        enrollmentService.createApplication(body);
        return Result.success(Map.of("created", 1));
    }

    @Operation(summary = "更新报名记录")
    @PutMapping("/applications/{id}")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Void> updateApplication(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        enrollmentService.updateApplication(id, body);
        return Result.success();
    }

    @Operation(summary = "删除报名记录")
    @DeleteMapping("/applications/{id}")
    @CasbinAccess(resource = "enrollment", action = "edit")
    public Result<Void> deleteApplication(@PathVariable Long id) {
        enrollmentService.deleteApplication(id);
        return Result.success();
    }

    @Operation(summary = "录取")
    @PostMapping("/applications/{id}/admit")
    @CasbinAccess(resource = "enrollment", action = "edit")
    @Audited(module = "enrollment", action = "ADMIT", resourceType = "Application", description = "录取报名")
    public Result<Void> admitApplication(@PathVariable Long id) {
        enrollmentService.admitApplication(id);
        return Result.success();
    }

    @Operation(summary = "不录取")
    @PostMapping("/applications/{id}/reject")
    @CasbinAccess(resource = "enrollment", action = "edit")
    @Audited(module = "enrollment", action = "REJECT", resourceType = "Application", description = "拒绝报名")
    public Result<Void> rejectApplication(@PathVariable Long id,
                                           @RequestBody(required = false) Map<String, Object> body) {
        String comment = body != null ? (String) body.get("comment") : null;
        enrollmentService.rejectApplication(id, comment);
        return Result.success();
    }

    @Operation(summary = "报到注册（创建学生记录+分配班级）")
    @PostMapping("/applications/{id}/register")
    @CasbinAccess(resource = "enrollment", action = "edit")
    @Audited(module = "enrollment", action = "REGISTER", resourceType = "Application", description = "报到注册")
    public Result<Map<String, Object>> registerApplication(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long orgUnitId = toLong(body.get("orgUnitId"));
        Map<String, Object> result = enrollmentService.registerApplication(id, orgUnitId);
        String error = (String) result.get("error");
        if (error != null) {
            return Result.error(error);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("studentId", result.get("studentId"));
        out.put("studentNo", result.get("studentNo"));
        return Result.success(out);
    }

    @Operation(summary = "批量录取")
    @PostMapping("/applications/batch-admit")
    @CasbinAccess(resource = "enrollment", action = "edit")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> batchAdmit(@RequestBody Map<String, Object> body) {
        List<Number> ids = (List<Number>) body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要录取的报名记录");
        }
        int count = enrollmentService.batchAdmit(ids);
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

        List<Map<String, Object>> records = enrollmentService.exportApplicationsRaw(year, majorId, status);
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

    // ==================== Private Helpers (presentation only) ====================

    private String str(Object val) {
        return val != null ? val.toString() : "";
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.valueOf(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
