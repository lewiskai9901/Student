package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.dto.PermissionCreateRequest;
import com.school.management.dto.PermissionUpdateRequest;
import com.school.management.entity.Permission;
import com.school.management.service.PermissionService;
import com.school.management.util.SecurityUtils;
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
 * 权限管理控制器 (V1 - 已弃用)
 *
 * @author system
 * @since 1.0.0
 * @deprecated 使用 V2 API {@link com.school.management.interfaces.rest.access.PermissionController} 替代
 *             V2 API 路径: /api/v2/permissions
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@Slf4j
@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@Tag(name = "权限管理 (已弃用)", description = "权限管理相关接口 - 请使用 /api/v2/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 创建权限
     */
    @PostMapping
    @Operation(summary = "创建权限", description = "创建新的权限")
    @PreAuthorize("hasAuthority('system:permission:add')")
    public Result<Long> createPermission(@Valid @RequestBody PermissionCreateRequest request) {
        log.info("创建权限请求: {}", request.getPermissionCode());
        Long permissionId = permissionService.createPermission(request);
        return Result.success(permissionId);
    }

    /**
     * 更新权限
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新权限", description = "更新指定权限信息")
    @PreAuthorize("hasAuthority('system:permission:edit')")
    public Result<Void> updatePermission(
            @Parameter(description = "权限ID") @PathVariable Long id,
            @Valid @RequestBody PermissionUpdateRequest request) {
        log.info("更新权限: {}", id);
        request.setId(id);
        permissionService.updatePermission(request);
        return Result.success();
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除权限", description = "删除指定权限")
    @PreAuthorize("hasAuthority('system:permission:delete')")
    public Result<Void> deletePermission(@Parameter(description = "权限ID") @PathVariable Long id) {
        log.info("删除权限: {}", id);
        permissionService.deletePermission(id);
        return Result.success();
    }

    /**
     * 批量删除权限
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除权限", description = "批量删除多个权限")
    @PreAuthorize("hasAuthority('system:permission:delete')")
    public Result<Void> deletePermissions(@RequestBody List<Long> ids) {
        log.info("批量删除权限: {}", ids);
        permissionService.deletePermissions(ids);
        return Result.success();
    }

    /**
     * 根据ID获取权限
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取权限详情", description = "根据ID获取权限详细信息")
    @PreAuthorize("hasAuthority('system:permission:view')")
    public Result<Permission> getPermissionById(@Parameter(description = "权限ID") @PathVariable Long id) {
        Permission permission = permissionService.getPermissionById(id);
        return Result.success(permission);
    }

    /**
     * 获取权限树形结构
     */
    @GetMapping("/tree")
    @Operation(summary = "获取权限树", description = "获取权限树形结构")
    @PreAuthorize("hasAuthority('system:permission:view')")
    public Result<List<Permission>> getPermissionTree() {
        List<Permission> tree = permissionService.getPermissionTree();
        return Result.success(tree);
    }

    /**
     * 获取所有权限列表
     */
    @GetMapping
    @Operation(summary = "获取所有权限", description = "获取所有权限列表")
    @PreAuthorize("hasAuthority('system:permission:view')")
    public Result<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return Result.success(permissions);
    }

    /**
     * 根据父权限ID获取子权限列表
     */
    @GetMapping("/children/{parentId}")
    @Operation(summary = "获取子权限", description = "根据父权限ID获取子权限列表")
    @PreAuthorize("hasAuthority('system:permission:view')")
    public Result<List<Permission>> getPermissionsByParentId(
            @Parameter(description = "父权限ID") @PathVariable Long parentId) {
        List<Permission> permissions = permissionService.getPermissionsByParentId(parentId);
        return Result.success(permissions);
    }

    /**
     * 检查权限编码是否存在
     */
    @GetMapping("/exists")
    @Operation(summary = "检查权限编码", description = "检查权限编码是否已存在")
    @PreAuthorize("hasAuthority('system:permission:view')")
    public Result<Boolean> existsPermissionCode(
            @Parameter(description = "权限编码") @RequestParam String permissionCode,
            @Parameter(description = "排除的权限ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = permissionService.existsPermissionCode(permissionCode, excludeId);
        return Result.success(exists);
    }

    /**
     * 获取当前用户的权限编码列表
     */
    @GetMapping("/my-permissions")
    @Operation(summary = "获取我的权限", description = "获取当前用户的权限编码列表")
    public Result<List<String>> getMyPermissions() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<String> permissionCodes = permissionService.getUserPermissionCodes(userId);
        return Result.success(permissionCodes);
    }
}