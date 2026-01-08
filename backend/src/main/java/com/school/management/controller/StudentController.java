package com.school.management.controller;

import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.dto.StudentCreateRequest;
import com.school.management.dto.StudentQueryRequest;
import com.school.management.dto.StudentResponse;
import com.school.management.dto.StudentUpdateRequest;
import com.school.management.service.StudentService;
import com.school.management.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 学生控制器 (V1 - 已弃用)
 *
 * @author system
 * @since 1.0.0
 * @deprecated 使用 V2 API {@link com.school.management.interfaces.rest.student.StudentController} 替代
 *             V2 API 路径: /api/v2/students
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@Slf4j
@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@Tag(name = "学生管理 (已弃用)", description = "学生信息管理相关接口 - 请使用 /api/v2/students")
public class StudentController {

    private final StudentService studentService;

    /**
     * 创建学生
     */
    @PostMapping
    @Operation(summary = "创建学生", description = "创建新的学生信息")
    @PreAuthorize("hasAuthority('student:info:add')")
    @OperationLog(module = "student", type = "create", name = "创建学生")
    public Result<Long> createStudent(@Valid @RequestBody StudentCreateRequest request) {
        log.info("创建学生请求: {}", request.getStudentNo());
        Long studentId = studentService.createStudent(request);
        return Result.success(studentId);
    }

    /**
     * 更新学生信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新学生信息", description = "更新指定学生的详细信息")
    @PreAuthorize("hasAuthority('student:info:edit')")
    @OperationLog(module = "student", type = "update", name = "更新学生")
    public Result<Void> updateStudent(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Valid @RequestBody StudentUpdateRequest request) {
        log.info("更新学生信息: {}", id);
        request.setId(id);
        studentService.updateStudent(request);
        return Result.success();
    }

    /**
     * 删除学生
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除学生", description = "删除指定的学生信息")
    @PreAuthorize("hasAuthority('student:info:delete')")
    @OperationLog(module = "student", type = "delete", name = "删除学生")
    public Result<Void> deleteStudent(@Parameter(description = "学生ID") @PathVariable Long id) {
        log.info("删除学生: {}", id);
        studentService.deleteStudent(id);
        return Result.success();
    }

    /**
     * 批量删除学生
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除学生", description = "批量删除多个学生信息")
    @PreAuthorize("hasAuthority('student:info:delete')")
    public Result<Void> deleteStudents(@RequestBody List<Long> ids) {
        log.info("批量删除学生: {}", ids);
        studentService.deleteStudents(ids);
        return Result.success();
    }

    /**
     * 根据ID获取学生信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取学生信息", description = "根据ID获取学生详细信息")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<StudentResponse> getStudentById(@Parameter(description = "学生ID") @PathVariable Long id) {
        StudentResponse student = studentService.getStudentById(id);
        return Result.success(student);
    }

    /**
     * 根据学号获取学生信息
     */
    @GetMapping("/by-no/{studentNo}")
    @Operation(summary = "根据学号获取学生", description = "根据学号获取学生信息")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<StudentResponse> getStudentByNo(@Parameter(description = "学号") @PathVariable String studentNo) {
        StudentResponse student = studentService.getStudentByNo(studentNo);
        return Result.success(student);
    }

    /**
     * 分页查询学生
     */
    @GetMapping
    @Operation(summary = "分页查询学生", description = "根据条件分页查询学生信息")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<PageResult<StudentResponse>> getStudentPage(StudentQueryRequest request) {
        PageResult<StudentResponse> result = studentService.getStudentPage(request);
        return Result.success(result);
    }

    /**
     * 快速搜索学生(用于选择器)
     */
    @GetMapping("/search")
    @Operation(summary = "快速搜索学生", description = "根据姓名或学号快速搜索学生,返回简化信息")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<List<StudentResponse>> searchStudents(
            @Parameter(description = "搜索关键字(姓名或学号)") @RequestParam String keyword,
            @Parameter(description = "班级ID") @RequestParam(required = false) Long classId,
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("快速搜索学生: keyword={}, classId={}, limit={}", keyword, classId, limit);
        List<StudentResponse> students = studentService.searchStudents(keyword, classId, limit);
        return Result.success(students);
    }

    /**
     * 检查学号是否存在
     */
    @GetMapping("/exists")
    @Operation(summary = "检查学号", description = "检查学号是否已存在")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<Boolean> existsStudentNo(
            @Parameter(description = "学号") @RequestParam String studentNo,
            @Parameter(description = "排除的学生ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = studentService.existsStudentNo(studentNo, excludeId);
        return Result.success(exists);
    }

    /**
     * 更新学生状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "更新学生状态", description = "更新学生的在读状态")
    @PreAuthorize("hasAuthority('student:info:edit')")
    public Result<Void> updateStudentStatus(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam Integer status) {
        log.info("更新学生状态: {} -> {}", id, status);
        studentService.updateStudentStatus(id, status);
        return Result.success();
    }

    /**
     * 分配宿舍
     */
    @PatchMapping("/{id}/dormitory")
    @Operation(summary = "分配宿舍", description = "为学生分配宿舍")
    @PreAuthorize("hasAuthority('student:info:edit')")
    public Result<Void> assignDormitory(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Parameter(description = "宿舍ID") @RequestParam Long dormitoryId,
            @Parameter(description = "床位号") @RequestParam(required = false) String bedNumber) {
        log.info("分配宿舍: {} -> {} - {}", id, dormitoryId, bedNumber);
        studentService.assignDormitory(id, dormitoryId, bedNumber);
        return Result.success();
    }

    /**
     * 转班
     */
    @PatchMapping("/{id}/transfer")
    @Operation(summary = "学生转班", description = "将学生转到新班级")
    @PreAuthorize("hasAuthority('student:info:edit')")
    public Result<Void> transferClass(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Parameter(description = "新班级ID") @RequestParam Long newClassId) {
        log.info("学生转班: {} -> {}", id, newClassId);
        studentService.transferClass(id, newClassId);
        return Result.success();
    }

    /**
     * 重置密码
     */
    @PatchMapping("/{id}/reset-password")
    @Operation(summary = "重置密码", description = "重置学生登录密码")
    @PreAuthorize("hasAuthority('student:info:edit')")
    public Result<Void> resetPassword(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Parameter(description = "新密码") @RequestBody java.util.Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            throw new com.school.management.exception.BusinessException("新密码不能为空");
        }
        log.info("重置学生密码: {}", id);
        studentService.resetPassword(id, newPassword);
        return Result.success();
    }

    /**
     * 统计班级学生数量
     */
    @GetMapping("/count/by-class")
    @Operation(summary = "统计班级学生数", description = "统计指定班级的学生数量")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<Integer> countStudentsByClass(@Parameter(description = "班级ID") @RequestParam Long classId) {
        Integer count = studentService.countStudentsByClassId(classId);
        return Result.success(count);
    }

    /**
     * 统计宿舍学生数量
     */
    @GetMapping("/count/by-dormitory")
    @Operation(summary = "统计宿舍学生数", description = "统计指定宿舍的学生数量")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<Integer> countStudentsByDormitory(@Parameter(description = "宿舍ID") @RequestParam Long dormitoryId) {
        Integer count = studentService.countStudentsByDormitoryId(dormitoryId);
        return Result.success(count);
    }

    /**
     * 导出学生数据到Excel
     */
    @GetMapping("/export")
    @Operation(summary = "导出学生数据", description = "导出学生数据到Excel文件")
    @PreAuthorize("hasAuthority('student:info:export')")
    public ResponseEntity<byte[]> exportStudents(StudentQueryRequest request) throws IOException {
        log.info("导出学生数据");
        byte[] excelData = studentService.exportStudents(request);

        String filename = "学生信息_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(excelData);
    }

    /**
     * 从Excel导入学生数据
     */
    @PostMapping("/import")
    @Operation(summary = "导入学生数据", description = "从Excel文件导入学生数据")
    @PreAuthorize("hasAuthority('student:info:import')")
    public Result<String> importStudents(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("导入学生数据: {}", file.getOriginalFilename());
        String result = studentService.importStudents(file);
        return Result.success(result);
    }

    /**
     * 下载学生导入模板
     */
    @GetMapping("/template")
    @Operation(summary = "下载导入模板", description = "下载学生数据导入Excel模板")
    @PreAuthorize("hasAuthority('student:info:view')")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        log.info("下载学生导入模板");
        byte[] template = studentService.getImportTemplate();

        String filename = "学生信息导入模板.xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(template);
    }

    /**
     * 根据班级ID获取学生列表
     */
    @GetMapping("/by-class/{classId}")
    @Operation(summary = "获取班级学生列表", description = "根据班级ID获取该班级所有学生")
    @PreAuthorize("hasAuthority('student:info:view')")
    public Result<List<StudentResponse>> getStudentsByClass(@Parameter(description = "班级ID") @PathVariable Long classId) {
        log.info("根据班级ID获取学生列表: {}", classId);
        List<StudentResponse> students = studentService.getStudentsByClassId(classId);
        return Result.success(students);
    }
}