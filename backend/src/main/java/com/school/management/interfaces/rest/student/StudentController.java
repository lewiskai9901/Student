package com.school.management.interfaces.rest.student;

import com.school.management.application.student.StatusChangeRecordService;
import com.school.management.application.student.StudentApplicationService;
import com.school.management.application.student.command.*;
import com.school.management.application.student.query.StudentDTO;
import com.school.management.application.student.query.StudentQueryCriteria;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.interfaces.rest.student.dto.EnrollStudentRequest;
import com.school.management.interfaces.rest.student.dto.UpdateStudentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生管理 REST API控制器 (DDD架构)
 */
@Tag(name = "Student Management V2", description = "学生管理API - DDD架构")
@RestController("studentController")
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentApplicationService studentService;
    private final StatusChangeRecordService statusChangeRecordService;

    // ==================== 基础CRUD ====================

    @Operation(summary = "分页查询学生")
    @GetMapping
    @CasbinAccess(resource = "student:info", action = "view")
    public Result<PageResult<StudentDTO>> getStudents(
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "班级ID") @RequestParam(required = false) Long classId,
            @Parameter(description = "组织单元ID") @RequestParam(required = false) Long orgUnitId,
            @Parameter(description = "年级") @RequestParam(required = false) Integer gradeLevel,
            @Parameter(description = "状态") @RequestParam(required = false) Integer studentStatus,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {

        StudentQueryCriteria criteria = new StudentQueryCriteria();
        criteria.setKeyword(keyword);
        criteria.setClassId(classId);
        criteria.setOrgUnitId(orgUnitId);
        criteria.setGradeLevel(gradeLevel);
        criteria.setStatus(studentStatus);
        criteria.setPageNum(pageNum);
        criteria.setPageSize(pageSize);

        PageResult<StudentDTO> result = studentService.findByPage(criteria);
        return Result.success(result);
    }

    @Operation(summary = "获取学生详情")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "student:info", action = "view")
    public Result<StudentDTO> getStudent(
            @Parameter(description = "学生ID") @PathVariable Long id) {
        StudentDTO result = studentService.getById(id);
        return Result.success(result);
    }

    @Operation(summary = "根据学号获取学生")
    @GetMapping("/by-no/{studentNo}")
    @CasbinAccess(resource = "student:info", action = "view")
    public Result<StudentDTO> getStudentByNo(
            @Parameter(description = "学号") @PathVariable String studentNo) {
        StudentDTO result = studentService.getByStudentNo(studentNo);
        return Result.success(result);
    }

    @Operation(summary = "创建学生(入学)")
    @PostMapping
    @CasbinAccess(resource = "student:info", action = "add")
    public Result<Long> createStudent(@Valid @RequestBody EnrollStudentRequest request) {
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
    @CasbinAccess(resource = "student:info", action = "edit")
    public Result<Void> updateStudent(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
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
    @CasbinAccess(resource = "student:info", action = "delete")
    public Result<Void> deleteStudent(
            @Parameter(description = "学生ID") @PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.success();
    }

    @Operation(summary = "批量删除学生")
    @DeleteMapping("/batch")
    @CasbinAccess(resource = "student:info", action = "delete")
    public Result<Void> deleteStudents(@RequestBody List<Long> ids) {
        studentService.deleteStudents(ids);
        return Result.success();
    }

    // ==================== 状态操作 ====================

    @Operation(summary = "更新学生状态")
    @PatchMapping("/{id}/status")
    @CasbinAccess(resource = "student:info", action = "edit")
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
    @CasbinAccess(resource = "student:info", action = "edit")
    public Result<Void> suspendStudent(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Parameter(description = "原因") @RequestParam(required = false) String reason) {
        studentService.suspend(id, reason);
        return Result.success();
    }

    @Operation(summary = "学生复学")
    @PostMapping("/{id}/resume")
    @CasbinAccess(resource = "student:info", action = "edit")
    public Result<Void> resumeStudent(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Parameter(description = "原因") @RequestParam(required = false) String reason) {
        studentService.resume(id, reason);
        return Result.success();
    }

    @Operation(summary = "学生毕业")
    @PostMapping("/{id}/graduate")
    @CasbinAccess(resource = "student:info", action = "edit")
    public Result<Void> graduateStudent(
            @Parameter(description = "学生ID") @PathVariable Long id) {
        studentService.graduate(id);
        return Result.success();
    }

    @Operation(summary = "学生退学")
    @PostMapping("/{id}/withdraw")
    @CasbinAccess(resource = "student:info", action = "edit")
    public Result<Void> withdrawStudent(
            @Parameter(description = "学生ID") @PathVariable Long id,
            @Parameter(description = "原因") @RequestParam(required = false) String reason) {
        studentService.withdraw(id, reason);
        return Result.success();
    }

    // ==================== 班级和宿舍操作 ====================

    @Operation(summary = "学生转班")
    @PatchMapping("/{id}/transfer")
    @CasbinAccess(resource = "student:info", action = "edit")
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

    // ==================== 查询和统计 ====================
    // 注意: 宿舍分配/退出已统一到 /v9/places/{id}/check-in 和 /v9/places/{id}/check-out

    @Operation(summary = "根据班级获取学生列表")
    @GetMapping("/by-class/{classId}")
    @CasbinAccess(resource = "student:info", action = "view")
    public Result<List<StudentDTO>> getStudentsByClass(
            @Parameter(description = "班级ID") @PathVariable Long classId) {
        List<StudentDTO> result = studentService.findByClassId(classId);
        return Result.success(result);
    }

    @Operation(summary = "检查学号是否存在")
    @GetMapping("/exists")
    @CasbinAccess(resource = "student:info", action = "view")
    public Result<Boolean> existsStudentNo(
            @Parameter(description = "学号") @RequestParam String studentNo,
            @Parameter(description = "排除ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = studentService.existsByStudentNo(studentNo, excludeId);
        return Result.success(exists);
    }

    @Operation(summary = "统计班级学生数量")
    @GetMapping("/count/by-class")
    @CasbinAccess(resource = "student:info", action = "view")
    public Result<Long> countByClass(
            @Parameter(description = "班级ID") @RequestParam Long classId) {
        long count = studentService.countByClassId(classId);
        return Result.success(count);
    }

    @Operation(summary = "统计班级在读学生数量")
    @GetMapping("/count/active-by-class")
    @CasbinAccess(resource = "student:info", action = "view")
    public Result<Long> countActiveByClass(
            @Parameter(description = "班级ID") @RequestParam Long classId) {
        long count = studentService.countActiveByClassId(classId);
        return Result.success(count);
    }

    // ==================== 学籍异动记录 ====================

    @Operation(summary = "查看学生异动记录")
    @GetMapping("/{id}/status-changes")
    @CasbinAccess(resource = "student:info", action = "view")
    public Result<List<Map<String, Object>>> getStudentStatusChanges(
            @Parameter(description = "学生ID") @PathVariable Long id) {
        List<Map<String, Object>> changes = statusChangeRecordService.getStudentChanges(id);
        return Result.success(changes);
    }

    @Operation(summary = "查看所有异动记录（分页）")
    @GetMapping("/status-changes")
    @CasbinAccess(resource = "student:info", action = "view")
    public Result<PageResult<Map<String, Object>>> listStatusChanges(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "异动类型") @RequestParam(required = false) String changeType) {
        PageResult<Map<String, Object>> result = statusChangeRecordService.listRecentChanges(page, size, changeType);
        return Result.success(result);
    }

    // NOTE: Import/export and search functionality will be re-added via DDD application services.
    // V1 StudentService dependency has been removed.
}
