package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.dto.RoleCreateRequest;
import com.school.management.dto.RoleDataPermissionDTO;
import com.school.management.dto.RoleQueryRequest;
import com.school.management.dto.RoleUpdateRequest;
import com.school.management.entity.Role;
import com.school.management.service.RoleDataPermissionService;
import com.school.management.service.RoleService;
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
 * 角色管理控制器 (V1 - 已弃用)
 *
 * @author system
 * @since 1.0.0
 * @deprecated 使用 V2 API {@link com.school.management.interfaces.rest.access.RoleController} 替代
 *             V2 API 路径: /api/v2/roles
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@Slf4j
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "角色管理 (已弃用)", description = "角色管理相关接口 - 请使用 /api/v2/roles")
public class RoleController {

    private final RoleService roleService;
    private final RoleDataPermissionService roleDataPermissionService;

    /**
     * 创建角色
     */
    @PostMapping
    @Operation(summary = "创建角色", description = "创建新的角色")
    @PreAuthorize("hasAuthority('system:role:add')")
    @OperationLog(module = "system", type = "create", name = "创建角色")
    public Result<Long> createRole(@Valid @RequestBody RoleCreateRequest request) {
        log.info("创建角色请求: {}", request.getRoleCode());
        Long roleId = roleService.createRole(request);
        return Result.success(roleId);
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新角色", description = "更新指定角色信息")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @OperationLog(module = "system", type = "update", name = "更新角色")
    public Result<Void> updateRole(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        log.info("更新角色: {}", id);
        request.setId(id);
        roleService.updateRole(request);
        return Result.success();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色", description = "删除指定角色")
    @PreAuthorize("hasAuthority('system:role:delete')")
    @OperationLog(module = "system", type = "delete", name = "删除角色")
    public Result<Void> deleteRole(@Parameter(description = "角色ID") @PathVariable Long id) {
        log.info("删除角色: {}", id);
        roleService.deleteRole(id);
        return Result.success();
    }

    /**
     * 批量删除角色
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除角色", description = "批量删除多个角色")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public Result<Void> deleteRoles(@RequestBody List<Long> ids) {
        log.info("批量删除角色: {}", ids);
        roleService.deleteRoles(ids);
        return Result.success();
    }

    /**
     * 根据ID获取角色
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情", description = "根据ID获取角色详细信息")
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<Role> getRoleById(@Parameter(description = "角色ID") @PathVariable Long id) {
        Role role = roleService.getRoleById(id);
        return Result.success(role);
    }

    /**
     * 分页查询角色
     */
    @GetMapping
    @Operation(summary = "分页查询角色", description = "根据条件分页查询角色")
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<IPage<Role>> getRolePage(RoleQueryRequest request) {
        IPage<Role> result = roleService.getRolePage(request);
        return Result.success(result);
    }

    /**
     * 获取所有角色列表
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有角色", description = "获取所有启用的角色列表")
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return Result.success(roles);
    }

    /**
     * 检查角色编码是否存在
     */
    @GetMapping("/exists")
    @Operation(summary = "检查角色编码", description = "检查角色编码是否已存在")
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<Boolean> existsRoleCode(
            @Parameter(description = "角色编码") @RequestParam String roleCode,
            @Parameter(description = "排除的角色ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = roleService.existsRoleCode(roleCode, excludeId);
        return Result.success(exists);
    }

    /**
     * 分配权限给角色
     */
    @PostMapping("/{id}/permissions")
    @Operation(summary = "分配权限", description = "为角色分配权限")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Void> assignPermissions(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody List<Long> permissionIds) {
        log.info("为角色分配权限: {} -> {}", id, permissionIds);
        roleService.assignPermissions(id, permissionIds);
        return Result.success();
    }

    /**
     * 获取角色的权限ID列表
     */
    @GetMapping("/{id}/permissions")
    @Operation(summary = "获取角色权限", description = "获取角色的权限ID列表")
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<List<Long>> getRolePermissions(@Parameter(description = "角色ID") @PathVariable Long id) {
        List<Long> permissionIds = roleService.getRolePermissionIds(id);
        return Result.success(permissionIds);
    }

    /**
     * 分配用户给角色
     */
    @PostMapping("/{id}/users")
    @Operation(summary = "分配用户", description = "为角色分配用户")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Void> assignUsers(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody List<Long> userIds) {
        log.info("为角色分配用户: {} -> {}", id, userIds);
        roleService.assignUsers(id, userIds);
        return Result.success();
    }

    /**
     * 从角色中移除用户
     */
    @DeleteMapping("/{id}/users")
    @Operation(summary = "移除用户", description = "从角色中移除用户")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Void> removeUsers(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody List<Long> userIds) {
        log.info("从角色中移除用户: {} -> {}", id, userIds);
        roleService.removeUsers(id, userIds);
        return Result.success();
    }

    /**
     * 获取角色的数据权限配置
     */
    @GetMapping("/{id}/data-permissions")
    @Operation(summary = "获取数据权限", description = "获取角色的数据权限配置")
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<List<RoleDataPermissionDTO>> getRoleDataPermissions(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        List<RoleDataPermissionDTO> permissions = roleDataPermissionService.getRoleDataPermissions(id);
        return Result.success(permissions);
    }

    /**
     * 保存角色的数据权限配置
     */
    @PostMapping("/{id}/data-permissions")
    @Operation(summary = "保存数据权限", description = "保存角色的数据权限配置")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @OperationLog(module = "system", type = "update", name = "配置数据权限")
    public Result<Void> saveRoleDataPermissions(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody List<RoleDataPermissionDTO> permissions) {
        log.info("保存角色数据权限: {} -> {} 条配置", id, permissions.size());
        roleDataPermissionService.saveRoleDataPermissions(id, permissions);
        return Result.success();
    }
}