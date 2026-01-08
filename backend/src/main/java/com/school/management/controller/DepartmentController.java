package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.dto.DepartmentCreateRequest;
import com.school.management.dto.DepartmentResponse;
import com.school.management.service.DepartmentService;
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
 * 部门管理控制器 (V1 - 已弃用)
 *
 * @author system
 * @since 1.0.0
 * @deprecated 使用 V2 API {@link com.school.management.interfaces.rest.organization.OrgUnitController} 替代
 *             V2 API 路径: /api/v2/org-units
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@Slf4j
@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
@Tag(name = "部门管理 (已弃用)", description = "部门管理相关接口 - 请使用 /api/v2/org-units")
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 创建部门
     */
    @PostMapping
    @Operation(summary = "创建部门", description = "创建新的部门")
    @PreAuthorize("hasAuthority('student:department:add')")
    @OperationLog(module = "student", type = "create", name = "创建部门")
    public Result<Long> createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
        log.info("创建部门请求: {}", request.getDeptCode());
        Long departmentId = departmentService.createDepartment(request);
        return Result.success(departmentId);
    }

    /**
     * 更新部门
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新部门", description = "更新指定部门信息")
    @PreAuthorize("hasAuthority('student:department:edit')")
    @OperationLog(module = "student", type = "update", name = "更新部门")
    public Result<Void> updateDepartment(
            @Parameter(description = "部门ID") @PathVariable Long id,
            @Valid @RequestBody DepartmentCreateRequest request) {
        log.info("更新部门: {}", id);
        departmentService.updateDepartment(id, request);
        return Result.success();
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除部门", description = "删除指定的部门")
    @PreAuthorize("hasAuthority('student:department:delete')")
    @OperationLog(module = "student", type = "delete", name = "删除部门")
    public Result<Void> deleteDepartment(@Parameter(description = "部门ID") @PathVariable Long id) {
        log.info("删除部门: {}", id);
        departmentService.deleteDepartment(id);
        return Result.success();
    }

    /**
     * 根据ID获取部门
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取部门详情", description = "根据ID获取部门详细信息")
    @PreAuthorize("hasAuthority('student:department:view')")
    public Result<DepartmentResponse> getDepartmentById(@Parameter(description = "部门ID") @PathVariable Long id) {
        DepartmentResponse department = departmentService.getDepartmentById(id);
        return Result.success(department);
    }

    /**
     * 分页查询部门列表
     */
    @GetMapping
    @Operation(summary = "分页查询部门", description = "分页查询部门列表")
    @PreAuthorize("hasAuthority('student:department:view')")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<DepartmentResponse>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "部门名称") @RequestParam(required = false) String deptName,
            @Parameter(description = "部门编码") @RequestParam(required = false) String deptCode,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DepartmentResponse> page = departmentService.page(pageNum, pageSize, deptName, deptCode, status);
        return Result.success(page);
    }

    /**
     * 获取部门树形结构
     */
    @GetMapping("/tree")
    @Operation(summary = "获取部门树", description = "获取部门树形结构")
    @PreAuthorize("hasAuthority('student:department:view')")
    public Result<List<DepartmentResponse>> getDepartmentTree() {
        List<DepartmentResponse> tree = departmentService.getDepartmentTree();
        return Result.success(tree);
    }

    /**
     * 获取所有启用的部门
     */
    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的部门", description = "获取所有状态为启用的部门")
    @PreAuthorize("hasAuthority('student:department:view')")
    public Result<List<DepartmentResponse>> getAllEnabledDepartments() {
        List<DepartmentResponse> departments = departmentService.getAllEnabledDepartments();
        return Result.success(departments);
    }

    /**
     * 根据父部门ID获取子部门
     */
    @GetMapping("/children/{parentId}")
    @Operation(summary = "获取子部门", description = "根据父部门ID获取子部门列表")
    @PreAuthorize("hasAuthority('student:department:view')")
    public Result<List<DepartmentResponse>> getDepartmentsByParentId(
            @Parameter(description = "父部门ID") @PathVariable Long parentId) {
        List<DepartmentResponse> departments = departmentService.getDepartmentsByParentId(parentId);
        return Result.success(departments);
    }

    /**
     * 检查部门编码是否存在
     */
    @GetMapping("/exists")
    @Operation(summary = "检查部门编码", description = "检查部门编码是否已存在")
    @PreAuthorize("hasAuthority('student:department:view')")
    public Result<Boolean> existsDeptCode(
            @Parameter(description = "部门编码") @RequestParam String deptCode,
            @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = departmentService.existsDeptCode(deptCode, excludeId);
        return Result.success(exists);
    }

    /**
     * 更新部门状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "更新部门状态", description = "更新部门的启用状态")
    @PreAuthorize("hasAuthority('student:department:edit')")
    public Result<Void> updateStatus(
            @Parameter(description = "部门ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam Integer status) {
        log.info("更新部门状态: {} -> {}", id, status);
        departmentService.updateStatus(id, status);
        return Result.success();
    }
}