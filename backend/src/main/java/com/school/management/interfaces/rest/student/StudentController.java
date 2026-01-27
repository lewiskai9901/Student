package com.school.management.interfaces.rest.student;

import com.school.management.application.student.StudentApplicationService;
import com.school.management.application.student.command.*;
import com.school.management.application.student.query.StudentDTO;
import com.school.management.application.student.query.StudentQueryCriteria;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.dto.StudentQueryRequest;
import com.school.management.dto.StudentResponse;
import com.school.management.interfaces.rest.student.dto.EnrollStudentRequest;
import com.school.management.interfaces.rest.student.dto.UpdateStudentRequest;
import com.school.management.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 学生管理 REST API控制器 (DDD架构)
 */
@Tag(name = "Student Management V2", description = "学生管理API - DDD架构")
@RestController("studentControllerV2")
@RequestMapping("/v2/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentApplicationService studentService;
    private final StudentService studentServiceV1; // V1服务，用于导入导出等功能

    // ==================== 基础CRUD ====================

    @Operation(summary = "分页查询学生")
    @GetMapping
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<PageResult<StudentDTO>> getStudents(
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "班级ID") @RequestParam(required = false) Long classId,
            @Parameter(description = "组织单元ID") @RequestParam(required = false) Long orgUnitId,
            @Parameter(description = "年级") @RequestParam(required = false) Integer gradeLevel,
            @Parameter(description = "宿舍ID") @RequestParam(required = false) Long dormitoryId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer studentStatus,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {

        StudentQueryCriteria criteria = new StudentQueryCriteria();
        criteria.setKeyword(keyword);
        criteria.setClassId(classId);
        criteria.setOrgUnitId(orgUnitId);
        criteria.setGradeLevel(gradeLevel);
        criteria.setDormitoryId(dormitoryId);
        criteria.setStatus(studentStatus);
        criteria.setPageNum(pageNum);
        criteria.setPageSize(pageSize);

        PageResult<StudentDTO> result = studentService.findByPage(criteria);
        return Result.success(result);
    }

    @Operation(summary = "获取学生详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<StudentDTO> getStudent(
            @Parameter(description = "学生ID") @PathVariable Long id) {
        StudentDTO result = studentService.getById(id);
        return Result.success(result);
    }

    @Operation(summary = "根据学号获取学生")
    @GetMapping("/by-no/{studentNo}")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<StudentDTO> getStudentByNo(
            @Parameter(description = "学号") @PathVariable String studentNo) {
        StudentDTO result = studentService.getByStudentNo(studentNo);
        return Result.success(result);
    }

    @Operation(summary = "创建学生(入学)")
    @PostMapping
    @PreAuthorize("hasAuthority('student:create')")
    public Result<Long> createStudent(@RequestBody EnrollStudentRequest request) {
        EnrollStudentCommand command = EnrollStudentCommand.builder()
                .studentNo(request.getStudentNo())
                .name(request.getName())
                .gender(request.getGender())
                .idCard(request.getIdCard())
                .phone(request.getPhone())
                .email(request.getEmail())
                .birthDate(request.getBirthDate())
                .enrollmentDate(request.getEnrollmentDate())
                .classId(request.getClassId())
                .dormitoryId(request.getDormitoryId())
                .bedNumber(request.getBedNumber())
                .homeAddress(request.getHomeAddress())
                .emergencyContact(request.getEmergencyContact())
                .emergencyPhone(request.getEmergencyPhone())
                .remark(request.getRemark())
                .build();

        Long id = studentService.enrollStudent(command);
        return Result.success(id);
    }

    @Operation(summary = "更新学生信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('student:update')")
    public Result<Void> updateStudent(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @RequestBody UpdateStudentRequest request) {
        UpdateStudentCommand command = UpdateStudentCommand.builder()
                .id(id)
                .studentNo(request.getStudentNo())
                .name(request.getName())
                .gender(request.getGender())
                .phone(request.getPhone())
                .email(request.getEmail())
                .birthDate(request.getBirthDate())
                .homeAddress(request.getHomeAddress())
                .emergencyContact(request.getEmergencyContact())
                .emergencyPhone(request.getEmergencyPhone())
                .remark(request.getRemark())
                .build();

        studentService.updateStudent(command);
        return Result.success();
    }

    @Operation(summary = "删除学生")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('student:delete')")
    public Result<Void> deleteStudent(
            @Parameter(description = "学生ID") @PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.success();
    }

    @Operation(summary = "批量删除学生")
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('student:delete')")
    public Result<Void> deleteStudents(@RequestBody List<Long> ids) {
        studentService.deleteStudents(ids);
        return Result.success();
    }

    // ==================== 状态操作 ====================

    @Operation(summary = "更新学生状态")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('student:update')")
    public Result<Void> updateStatus(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam Integer status,
            @Parameter(description = "原因") @RequestParam(required = false) String reason) {
        ChangeStudentStatusCommand command = ChangeStudentStatusCommand.builder()
                .studentId(id)
                .newStatus(com.school.management.domain.student.model.valueobject.StudentStatus.fromCode(status))
                .reason(reason)
                .build();
        studentService.changeStatus(command);
        return Result.success();
    }

    @Operation(summary = "学生休学")
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('student:update')")
    public Result<Void> suspendStudent(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Parameter(description = "原因") @RequestParam(required = false) String reason) {
        studentService.suspend(id, reason);
        return Result.success();
    }

    @Operation(summary = "学生复学")
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('student:update')")
    public Result<Void> resumeStudent(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Parameter(description = "原因") @RequestParam(required = false) String reason) {
        studentService.resume(id, reason);
        return Result.success();
    }

    @Operation(summary = "学生毕业")
    @PostMapping("/{id}/graduate")
    @PreAuthorize("hasAuthority('student:update')")
    public Result<Void> graduateStudent(
            @Parameter(description = "学生ID") @PathVariable Long id) {
        studentService.graduate(id);
        return Result.success();
    }

    @Operation(summary = "学生退学")
    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAuthority('student:update')")
    public Result<Void> withdrawStudent(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Parameter(description = "原因") @RequestParam(required = false) String reason) {
        studentService.withdraw(id, reason);
        return Result.success();
    }

    // ==================== 班级和宿舍操作 ====================

    @Operation(summary = "学生转班")
    @PatchMapping("/{id}/transfer")
    @PreAuthorize("hasAuthority('student:update')")
    public Result<Void> transferClass(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Parameter(description = "新班级ID") @RequestParam Long newClassId) {
        TransferClassCommand command = TransferClassCommand.builder()
                .studentId(id)
                .newClassId(newClassId)
                .build();
        studentService.transferClass(command);
        return Result.success();
    }

    @Operation(summary = "分配宿舍")
    @PatchMapping("/{id}/dormitory")
    @PreAuthorize("hasAuthority('student:update')")
    public Result<Void> assignDormitory(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Parameter(description = "宿舍ID") @RequestParam Long dormitoryId,
            @Parameter(description = "床位号") @RequestParam(required = false) Integer bedNumber) {
        AssignDormitoryCommand command = AssignDormitoryCommand.builder()
                .studentId(id)
                .dormitoryId(dormitoryId)
                .bedNumber(bedNumber)
                .build();
        studentService.assignDormitory(command);
        return Result.success();
    }

    @Operation(summary = "移除宿舍")
    @DeleteMapping("/{id}/dormitory")
    @PreAuthorize("hasAuthority('student:update')")
    public Result<Void> removeDormitory(
            @Parameter(description = "学生ID") @PathVariable Long id) {
        studentService.removeDormitory(id);
        return Result.success();
    }

    // ==================== 查询和统计 ====================

    @Operation(summary = "根据班级获取学生列表")
    @GetMapping("/by-class/{classId}")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<List<StudentDTO>> getStudentsByClass(
            @Parameter(description = "班级ID") @PathVariable Long classId) {
        List<StudentDTO> result = studentService.findByClassId(classId);
        return Result.success(result);
    }

    @Operation(summary = "根据宿舍获取学生列表")
    @GetMapping("/by-dormitory/{dormitoryId}")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<List<StudentDTO>> getStudentsByDormitory(
            @Parameter(description = "宿舍ID") @PathVariable Long dormitoryId) {
        List<StudentDTO> result = studentService.findByDormitoryId(dormitoryId);
        return Result.success(result);
    }

    @Operation(summary = "检查学号是否存在")
    @GetMapping("/exists")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<Boolean> existsStudentNo(
            @Parameter(description = "学号") @RequestParam String studentNo,
            @Parameter(description = "排除ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = studentService.existsByStudentNo(studentNo, excludeId);
        return Result.success(exists);
    }

    @Operation(summary = "统计班级学生数量")
    @GetMapping("/count/by-class")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<Long> countByClass(
            @Parameter(description = "班级ID") @RequestParam Long classId) {
        long count = studentService.countByClassId(classId);
        return Result.success(count);
    }

    @Operation(summary = "统计班级在读学生数量")
    @GetMapping("/count/active-by-class")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<Long> countActiveByClass(
            @Parameter(description = "班级ID") @RequestParam Long classId) {
        long count = studentService.countActiveByClassId(classId);
        return Result.success(count);
    }

    // ==================== 搜索、导入导出 (委托V1服务) ====================

    @Operation(summary = "快速搜索学生")
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<List<StudentResponse>> searchStudents(
            @Parameter(description = "关键字") @RequestParam String keyword,
            @Parameter(description = "班级ID") @RequestParam(required = false) Long classId,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") Integer limit) {
        List<StudentResponse> result = studentServiceV1.searchStudents(keyword, classId, limit);
        return Result.success(result);
    }

    @Operation(summary = "统计宿舍学生数量")
    @GetMapping("/count/by-dormitory")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<Integer> countByDormitory(
            @Parameter(description = "宿舍ID") @RequestParam Long dormitoryId) {
        Integer count = studentServiceV1.countStudentsByDormitoryId(dormitoryId);
        return Result.success(count);
    }

    @Operation(summary = "重置学生密码")
    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('student:update')")
    public Result<Void> resetPassword(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @RequestBody java.util.Map<String, String> request) {
        String newPassword = request.get("newPassword");
        studentServiceV1.resetPassword(id, newPassword);
        return Result.success();
    }

    @Operation(summary = "导出学生数据")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('student:info:export')")
    public ResponseEntity<byte[]> exportStudents(StudentQueryRequest request) throws IOException {
        byte[] data = studentServiceV1.exportStudents(request);
        String filename = "学生数据_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @Operation(summary = "下载导入模板")
    @GetMapping("/template")
    @PreAuthorize("hasAuthority('student:info:import')")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] data = studentServiceV1.getImportTemplate();
        String filename = "学生导入模板.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @Operation(summary = "导入学生数据")
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('student:info:import')")
    public Result<String> importStudents(@RequestParam("file") MultipartFile file) throws IOException {
        String result = studentServiceV1.importStudents(file);
        return Result.success(result);
    }

    @Operation(summary = "预览导入数据")
    @PostMapping("/import/preview")
    @PreAuthorize("hasAuthority('student:info:import')")
    public Result<Object> previewImportData(@RequestParam("file") MultipartFile file) throws IOException {
        Object result = studentServiceV1.previewImportData(file);
        return Result.success(result);
    }

    @Operation(summary = "确认导入数据")
    @PostMapping("/import/confirm")
    @PreAuthorize("hasAuthority('student:info:import')")
    public Result<Object> confirmImport(@RequestBody List<java.util.Map<String, Object>> data) throws IOException {
        Object result = studentServiceV1.confirmImport(data);
        return Result.success(result);
    }

    @Operation(summary = "导出导入失败的数据")
    @PostMapping("/import/export-failed")
    @PreAuthorize("hasAuthority('student:info:import')")
    public ResponseEntity<byte[]> exportFailedData(@RequestBody List<java.util.Map<String, Object>> data) throws IOException {
        byte[] result = studentServiceV1.exportFailedData(data);
        String filename = "导入失败数据_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(result);
    }
}
