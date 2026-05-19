package com.school.management.infrastructure.extension.plugins.education.interfaces.rest.student;

import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.extension.plugins.education.application.student.AttendanceApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.*;

/**
 * 考勤管理 REST Controller.
 * <p>M3.2.2 (2026-05-20): 21 处直 jdbc 已下沉到 AttendanceApplicationService.
 */
@Slf4j
@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceApplicationService attendanceService;

    // ==================== 考勤记录 CRUD ====================

    @PostMapping("/records")
    @CasbinAccess(resource = "student:attendance", action = "edit")
    public Result<Map<String, Object>> createRecord(@RequestBody Map<String, Object> body) {
        Long recordedBy = SecurityUtils.getCurrentUserId();
        Long semesterId = toLong(body.get("semesterId"));
        Long courseId = toLong(body.get("courseId"));
        Long orgUnitId = toLong(body.get("orgUnitId"));
        Long studentId = toLong(body.get("studentId"));
        String dateStr = (String) body.get("attendanceDate");
        Integer period = toInt(body.get("period"));
        Integer attendanceType = toInt(body.get("attendanceType"));
        if (attendanceType == null) attendanceType = 1;
        Integer status = toInt(body.get("status"));
        String checkMethod = body.get("checkMethod") != null ? (String) body.get("checkMethod") : "MANUAL";
        String remark = (String) body.get("remark");

        attendanceService.createRecord(semesterId, courseId, orgUnitId, studentId,
            dateStr, period, attendanceType, status, checkMethod, remark, recordedBy);
        return Result.success(Map.of("created", 1));
    }

    @GetMapping("/records")
    @CasbinAccess(resource = "student:attendance", action = "view")
    public Result<List<Map<String, Object>>> listRecords(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer attendanceType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer size) {
        return Result.success(attendanceService.listRecords(
            semesterId, orgUnitId, studentId, courseId, date, startDate, endDate,
            status, attendanceType, page, size));
    }

    @GetMapping("/records/by-class")
    @CasbinAccess(resource = "student:attendance", action = "view")
    public Result<List<Map<String, Object>>> getByClass(
            @RequestParam Long orgUnitId,
            @RequestParam String date,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer period) {
        return Result.success(attendanceService.getByClass(orgUnitId, date, courseId, period));
    }

    @PutMapping("/records/{id}")
    @CasbinAccess(resource = "student:attendance", action = "edit")
    public Result<Void> updateRecord(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = toInt(body.get("status"));
        String remark = (String) body.get("remark");
        attendanceService.updateRecord(id, status, remark);
        return Result.success();
    }

    @DeleteMapping("/records/{id}")
    @CasbinAccess(resource = "student:attendance", action = "edit")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        attendanceService.deleteRecord(id);
        return Result.success();
    }

    // ==================== 批量考勤 ====================

    @PostMapping("/batch")
    @CasbinAccess(resource = "student:attendance", action = "edit")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> batchRecord(@RequestBody Map<String, Object> body) {
        Long semesterId = toLong(body.get("semesterId"));
        Long orgUnitId = toLong(body.get("orgUnitId"));
        Long courseId = toLong(body.get("courseId"));
        String dateStr = (String) body.get("date");
        Integer period = toInt(body.get("period"));
        Integer attendanceType = toInt(body.get("attendanceType"));
        if (attendanceType == null) attendanceType = 1;
        List<Map<String, Object>> students = (List<Map<String, Object>>) body.get("user_student");

        int count = attendanceService.batchRecord(semesterId, orgUnitId, courseId, dateStr,
            period, attendanceType, students);
        return Result.success(Map.of("recorded", count));
    }

    // ==================== 考勤统计 ====================

    @GetMapping("/statistics")
    @CasbinAccess(resource = "student:attendance", action = "view")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(attendanceService.getStatistics(semesterId, orgUnitId, startDate, endDate));
    }

    @GetMapping("/statistics/student/{studentId}")
    @CasbinAccess(resource = "student:attendance", action = "view")
    public Result<Map<String, Object>> getStudentStatistics(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(attendanceService.getStudentStatistics(studentId, semesterId, startDate, endDate));
    }

    // ==================== 请假管理 ====================

    @PostMapping("/leave-requests")
    @CasbinAccess(resource = "student:attendance", action = "edit")
    public Result<Map<String, Object>> createLeaveRequest(@RequestBody Map<String, Object> body) {
        Long studentId = toLong(body.get("studentId"));
        Integer leaveType = toInt(body.get("leaveType"));
        String startDate = (String) body.get("startDate");
        String endDate = (String) body.get("endDate");
        Integer startPeriod = toInt(body.get("startPeriod"));
        Integer endPeriod = toInt(body.get("endPeriod"));
        String reason = (String) body.get("reason");
        String attachmentUrls = body.get("attachmentUrls") != null ? body.get("attachmentUrls").toString() : null;

        attendanceService.createLeaveRequest(studentId, leaveType, startDate, endDate,
            startPeriod, endPeriod, reason, attachmentUrls);
        return Result.success(Map.of("created", 1));
    }

    @GetMapping("/leave-requests")
    @CasbinAccess(resource = "student:attendance", action = "view")
    public Result<List<Map<String, Object>>> listLeaveRequests(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Integer approvalStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(attendanceService.listLeaveRequests(
            studentId, orgUnitId, approvalStatus, startDate, endDate));
    }

    @PostMapping("/leave-requests/{id}/approve")
    @CasbinAccess(resource = "student:attendance", action = "edit")
    public Result<Void> approveLeave(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String, Object> body) {
        Long approverId = SecurityUtils.getCurrentUserId();
        String comment = body != null ? (String) body.get("comment") : null;
        attendanceService.approveLeave(id, approverId, comment);
        return Result.success();
    }

    @PostMapping("/leave-requests/{id}/reject")
    @CasbinAccess(resource = "student:attendance", action = "edit")
    public Result<Void> rejectLeave(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, Object> body) {
        Long approverId = SecurityUtils.getCurrentUserId();
        String comment = body != null ? (String) body.get("comment") : null;
        attendanceService.rejectLeave(id, approverId, comment);
        return Result.success();
    }

    @GetMapping("/leave-requests/pending")
    @CasbinAccess(resource = "student:attendance", action = "view")
    public Result<List<Map<String, Object>>> pendingLeaves() {
        return Result.success(attendanceService.pendingLeaves());
    }

    // ==================== 导出 ====================

    @GetMapping("/records/export")
    @CasbinAccess(resource = "student:attendance", action = "view")
    public void exportAttendance(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {

        List<Map<String, Object>> records = attendanceService.queryExportRecords(
            semesterId, orgUnitId, startDate, endDate);

        String[] statusNames = {"", "出勤", "迟到", "早退", "请假", "旷课"};

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("考勤记录");

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

            String[] headers = {"日期", "学号", "姓名", "班级", "课程", "节次", "状态", "备注"};
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
                String[] values = {
                    str(r.get("attendance_date")), str(r.get("student_no")),
                    str(r.get("student_name")), str(r.get("class_name")),
                    str(r.get("course_name")),
                    r.get("period") != null ? "第" + r.get("period") + "节" : "",
                    st > 0 && st < statusNames.length ? statusNames[st] : "",
                    str(r.get("remark"))
                };
                for (int i = 0; i < values.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(values[i]);
                    cell.setCellStyle(cellStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=attendance.xlsx");
            wb.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    // ==================== Presentation Helpers ====================

    private String str(Object val) {
        return val != null ? val.toString() : "";
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.valueOf(val.toString());
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.valueOf(val.toString());
    }
}
