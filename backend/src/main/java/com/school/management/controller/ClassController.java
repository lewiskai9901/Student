package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.dto.ClassCreateRequest;
import com.school.management.dto.ClassQueryRequest;
import com.school.management.dto.ClassResponse;
import com.school.management.dto.ClassUpdateRequest;
import com.school.management.service.ClassService;
import com.school.management.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级管理控制器 (V1 - 已弃用)
 *
 * @author system
 * @since 1.0.0
 * @deprecated 使用 V2 API {@link com.school.management.interfaces.rest.organization.SchoolClassController} 替代
 *             V2 API 路径: /api/v2/organization/classes
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@Slf4j
@RestController
@RequestMapping("/classes")
@RequiredArgsConstructor
@Tag(name = "班级管理 (已弃用)", description = "班级管理相关接口 - 请使用 /api/v2/organization/classes")
public class ClassController {

    private final ClassService classService;

    /**
     * 创建班级
     */
    @PostMapping
    @Operation(summary = "创建班级", description = "创建新的班级")
    @PreAuthorize("hasAuthority('student:class:add')")
    @OperationLog(module = "student", type = "create", name = "创建班级")
    public Result<Long> createClass(@Valid @RequestBody ClassCreateRequest request) {
        log.info("创建班级请求: {}", request.getClassCode());
        Long classId = classService.createClass(request);
        return Result.success(classId);
    }

    /**
     * 更新班级
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新班级", description = "更新指定班级信息")
    @PreAuthorize("hasAuthority('student:class:edit')")
    @OperationLog(module = "student", type = "update", name = "更新班级")
    public Result<Void> updateClass(
            @Parameter(description = "班级ID") @PathVariable Long id,
            @Valid @RequestBody ClassUpdateRequest request) {
        log.info("更新班级: {}", id);
        request.setId(id);
        classService.updateClass(request);
        return Result.success();
    }

    /**
     * 删除班级
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除班级", description = "删除指定的班级")
    @PreAuthorize("hasAuthority('student:class:delete')")
    @OperationLog(module = "student", type = "delete", name = "删除班级")
    public Result<Void> deleteClass(@Parameter(description = "班级ID") @PathVariable Long id) {
        log.info("删除班级: {}", id);
        classService.deleteClass(id);
        return Result.success();
    }

    /**
     * 批量删除班级
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除班级", description = "批量删除多个班级")
    @PreAuthorize("hasAuthority('student:class:delete')")
    public Result<Void> deleteClasses(@RequestBody List<Long> ids) {
        log.info("批量删除班级: {}", ids);
        classService.deleteClasses(ids);
        return Result.success();
    }

    /**
     * 根据ID获取班级
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取班级详情", description = "根据ID获取班级详细信息")
    @PreAuthorize("hasAuthority('student:class:view')")
    public Result<ClassResponse> getClassById(@Parameter(description = "班级ID") @PathVariable Long id) {
        ClassResponse clazz = classService.getClassById(id);
        return Result.success(clazz);
    }

    /**
     * 分页查询班级
     */
    @GetMapping
    @Operation(summary = "分页查询班级", description = "根据条件分页查询班级")
    @PreAuthorize("hasAuthority('student:class:view')")
    public Result<PageResult<ClassResponse>> getClassPage(ClassQueryRequest request) {
        IPage<ClassResponse> page = classService.getClassPage(request);
        PageResult<ClassResponse> result = PageResult.from(page);
        return Result.success(result);
    }

    /**
     * 根据部门ID获取班级列表
     */
    @GetMapping("/by-department/{departmentId}")
    @Operation(summary = "根据部门获取班级", description = "根据部门ID获取班级列表")
    @PreAuthorize("hasAuthority('student:class:view')")
    public Result<List<ClassResponse>> getClassesByDepartmentId(
            @Parameter(description = "部门ID") @PathVariable Long departmentId) {
        List<ClassResponse> classes = classService.getClassesByDepartmentId(departmentId);
        return Result.success(classes);
    }

    /**
     * 根据班主任ID获取班级列表
     */
    @GetMapping("/by-teacher/{teacherId}")
    @Operation(summary = "根据班主任获取班级", description = "根据班主任ID获取班级列表")
    @PreAuthorize("hasAuthority('student:class:view')")
    public Result<List<ClassResponse>> getClassesByTeacherId(
            @Parameter(description = "班主任ID") @PathVariable Long teacherId) {
        List<ClassResponse> classes = classService.getClassesByTeacherId(teacherId);
        return Result.success(classes);
    }

    /**
     * 根据年级获取班级列表
     */
    @GetMapping("/by-grade/{gradeLevel}")
    @Operation(summary = "根据年级获取班级", description = "根据年级获取班级列表")
    @PreAuthorize("hasAuthority('student:class:view')")
    public Result<List<ClassResponse>> getClassesByGradeLevel(
            @Parameter(description = "年级") @PathVariable Integer gradeLevel) {
        List<ClassResponse> classes = classService.getClassesByGradeLevel(gradeLevel);
        return Result.success(classes);
    }

    /**
     * 获取所有启用的班级
     */
    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的班级", description = "获取所有状态为启用的班级")
    @PreAuthorize("hasAuthority('student:class:view')")
    public Result<List<ClassResponse>> getAllEnabledClasses() {
        List<ClassResponse> classes = classService.getAllEnabledClasses();
        return Result.success(classes);
    }

    /**
     * 获取所有班级(不分页)
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有班级", description = "获取所有班级，不分页")
    @PreAuthorize("hasAuthority('student:class:view')")
    public Result<List<ClassResponse>> getAllClasses() {
        List<ClassResponse> classes = classService.getAllEnabledClasses();
        return Result.success(classes);
    }

    /**
     * 检查班级编码是否存在
     */
    @GetMapping("/exists")
    @Operation(summary = "检查班级编码", description = "检查班级编码是否已存在")
    @PreAuthorize("hasAuthority('student:class:view')")
    public Result<Boolean> existsClassCode(
            @Parameter(description = "班级编码") @RequestParam String classCode,
            @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = classService.existsClassCode(classCode, excludeId);
        return Result.success(exists);
    }

    /**
     * 更新班级学生数量
     */
    @PatchMapping("/{id}/student-count")
    @Operation(summary = "更新班级学生数量", description = "更新班级的学生数量")
    @PreAuthorize("hasAuthority('student:class:edit')")
    public Result<Void> updateStudentCount(
            @Parameter(description = "班级ID") @PathVariable Long id,
            @Parameter(description = "学生数量") @RequestParam Integer studentCount) {
        log.info("更新班级学生数量: {} -> {}", id, studentCount);
        classService.updateStudentCount(id, studentCount);
        return Result.success();
    }

    /**
     * 更新班级状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "更新班级状态", description = "更新班级的启用状态")
    @PreAuthorize("hasAuthority('student:class:edit')")
    public Result<Void> updateStatus(
            @Parameter(description = "班级ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam Integer status) {
        log.info("更新班级状态: {} -> {}", id, status);
        classService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 设置班主任
     */
    @PostMapping("/{id}/assign-teacher")
    @Operation(summary = "设置班主任", description = "为班级设置班主任")
    @PreAuthorize("hasAuthority('student:class:edit')")
    public Result<Void> assignTeacher(
            @Parameter(description = "班级ID") @PathVariable Long id,
            @Parameter(description = "教师ID") @RequestParam(required = false) Long teacherId) {
        log.info("设置班主任: 班级ID={}, 教师ID={}", id, teacherId);
        classService.assignTeacher(id, teacherId);
        return Result.success();
    }

    /**
     * 为班级分配教室
     */
    @PostMapping("/{id}/assign-classroom")
    @Operation(summary = "为班级分配教室", description = "为指定班级分配教室")
    @PreAuthorize("hasAuthority('student:class:edit')")
    public Result<Void> assignClassroom(
            @Parameter(description = "班级ID") @PathVariable Long id,
            @Parameter(description = "教室ID") @RequestParam Long classroomId) {
        log.info("为班级分配教室: 班级ID={}, 教室ID={}", id, classroomId);
        classService.assignClassroom(id, classroomId);
        return Result.success();
    }

    /**
     * 取消班级教室分配
     */
    @DeleteMapping("/{id}/classroom")
    @Operation(summary = "取消教室分配", description = "取消班级的教室分配")
    @PreAuthorize("hasAuthority('student:class:edit')")
    public Result<Void> removeClassroom(@Parameter(description = "班级ID") @PathVariable Long id) {
        log.info("取消班级教室分配: {}", id);
        classService.removeClassroom(id);
        return Result.success();
    }

    /**
     * 为班级添加宿舍
     */
    @PostMapping("/{id}/dormitories")
    @Operation(summary = "为班级添加宿舍", description = "为班级添加宿舍分配")
    @PreAuthorize("hasAuthority('student:class:edit')")
    public Result<Void> addDormitory(
            @Parameter(description = "班级ID") @PathVariable Long id,
            @Parameter(description = "宿舍ID") @RequestParam Long dormitoryId,
            @Parameter(description = "分配床位数") @RequestParam Integer allocatedBeds) {
        log.info("为班级添加宿舍: 班级ID={}, 宿舍ID={}, 床位数={}", id, dormitoryId, allocatedBeds);
        classService.addDormitory(id, dormitoryId, allocatedBeds);
        return Result.success();
    }

    /**
     * 移除班级宿舍
     */
    @DeleteMapping("/{id}/dormitories/{dormitoryId}")
    @Operation(summary = "移除班级宿舍", description = "移除班级的宿舍分配")
    @PreAuthorize("hasAuthority('student:class:edit')")
    public Result<Void> removeDormitory(
            @Parameter(description = "班级ID") @PathVariable Long id,
            @Parameter(description = "宿舍ID") @PathVariable Long dormitoryId) {
        log.info("移除班级宿舍: 班级ID={}, 宿舍ID={}", id, dormitoryId);
        classService.removeDormitory(id, dormitoryId);
        return Result.success();
    }

    /**
     * 获取班级的宿舍列表
     */
    @GetMapping("/{id}/dormitories")
    @Operation(summary = "获取班级宿舍列表", description = "获取指定班级的所有宿舍")
    @PreAuthorize("hasAuthority('student:class:view')")
    public Result<List> getClassDormitories(@Parameter(description = "班级ID") @PathVariable Long id) {
        List dormitories = classService.getClassDormitories(id);
        return Result.success(dormitories);
    }

    /**
     * 获取班级的教室信息
     */
    @GetMapping("/{id}/classroom")
    @Operation(summary = "获取班级教室", description = "获取班级分配的教室信息")
    @PreAuthorize("hasAuthority('student:class:view')")
    public Result getClassClassroom(@Parameter(description = "班级ID") @PathVariable Long id) {
        Object classroom = classService.getClassClassroom(id);
        return Result.success(classroom);
    }
}