package com.school.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.Result;
import com.school.management.dto.UserCreateRequest;
import com.school.management.dto.UserQueryRequest;
import com.school.management.dto.UserResponse;
import com.school.management.dto.UserUpdateRequest;
import com.school.management.service.UserService;
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
 * 用户管理控制器 (V1 - 已弃用)
 *
 * @author system
 * @since 1.0.0
 * @deprecated 使用 V2 API {@link com.school.management.interfaces.rest.user.UserController} 替代
 *             V2 API 路径: /api/v2/users
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户管理 (已弃用)", description = "用户管理相关接口 - 请使用 /api/v2/users")
public class UserController {

    private final UserService userService;

    /**
     * 创建用户
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新的用户")
    @PreAuthorize("hasAuthority('system:user:add')")
    @OperationLog(module = "system", type = "create", name = "创建用户")
    public Result<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("创建用户请求: {}", request.getUsername());
        UserResponse response = userService.createUser(request);
        return Result.success(response);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "更新指定用户信息")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "更新用户")
    public Result<UserResponse> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("更新用户: {}", id);
        UserResponse response = userService.updateUser(id, request);
        return Result.success(response);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除指定用户")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @OperationLog(module = "system", type = "delete", name = "删除用户")
    public Result<Void> deleteUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("删除用户: {}", id);
        userService.deleteUser(id);
        return Result.success();
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除用户", description = "批量删除多个用户")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @OperationLog(module = "system", type = "delete", name = "批量删除用户")
    public Result<Void> deleteUsers(@RequestBody List<Long> ids) {
        log.info("批量删除用户: {}", ids);
        userService.deleteUsers(ids);
        return Result.success();
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<UserResponse> getUserById(@Parameter(description = "用户ID") @PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return Result.success(user);
    }

    /**
     * 分页查询用户
     */
    @GetMapping
    @Operation(summary = "分页查询用户", description = "根据条件分页查询用户")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<Page<UserResponse>> getUserPage(UserQueryRequest request) {
        Page<UserResponse> result = userService.getUserPage(request);
        return Result.success(result);
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置密码", description = "重置用户密码为随机强密码")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "重置用户密码")
    public Result<String> resetPassword(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("重置用户密码: {}", id);
        String newPassword = userService.resetPassword(id);
        return Result.success(newPassword);
    }

    /**
     * 启用/禁用用户
     */
    @PostMapping("/{id}/status")
    @Operation(summary = "更新用户状态", description = "启用或禁用用户")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "状态: 1启用 2禁用") @RequestParam Integer status) {
        log.info("更新用户状态: {} -> {}", id, status);
        userService.updateUserStatus(id, status);
        return Result.success();
    }

    /**
     * 检查用户名是否存在
     */
    @GetMapping("/exists")
    @Operation(summary = "检查用户名", description = "检查用户名是否已存在")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<Boolean> existsUsername(
            @Parameter(description = "用户名") @RequestParam String username,
            @Parameter(description = "排除的用户ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = userService.existsUsername(username, excludeId);
        return Result.success(exists);
    }

    /**
     * 获取用户的角色ID列表
     */
    @GetMapping("/{id}/roles")
    @Operation(summary = "获取用户角色", description = "获取用户的角色ID列表")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<Long>> getUserRoles(@Parameter(description = "用户ID") @PathVariable Long id) {
        List<Long> roleIds = userService.getUserRoleIds(id);
        return Result.success(roleIds);
    }

    /**
     * 分配角色给用户
     */
    @PostMapping("/{id}/roles")
    @Operation(summary = "分配角色", description = "为用户分配角色")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> assignRoles(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody List<Long> roleIds) {
        log.info("为用户分配角色: {} -> {}", id, roleIds);
        userService.assignRoles(id, roleIds);
        return Result.success();
    }

    /**
     * 获取简单用户列表（用于选择器）
     */
    @GetMapping("/simple")
    @Operation(summary = "获取简单用户列表", description = "获取简单用户信息列表，用于用户选择器")
    public Result<List<UserResponse>> getSimpleUserList(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        List<UserResponse> users = userService.getSimpleUserList(keyword);
        return Result.success(users);
    }

    /**
     * 按部门获取用户列表（用于任务分配选择器）
     */
    @GetMapping("/by-department/{departmentId}")
    @Operation(summary = "按部门获取用户列表", description = "获取指定部门的用户列表，支持包含子部门和关键词搜索")
    public Result<List<UserResponse>> getUsersByDepartment(
            @Parameter(description = "部门ID") @PathVariable Long departmentId,
            @Parameter(description = "是否包含子部门用户") @RequestParam(defaultValue = "false") Boolean includeChildren,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        log.info("按部门获取用户: departmentId={}, includeChildren={}, keyword={}", departmentId, includeChildren, keyword);
        List<UserResponse> users = userService.getUsersByDepartment(departmentId, includeChildren, keyword);
        return Result.success(users);
    }

    /**
     * 获取所有部门的用户列表（用于用户选择器）
     */
    @GetMapping("/with-departments")
    @Operation(summary = "获取带部门信息的用户列表", description = "获取所有启用用户，包含部门信息，支持关键词搜索")
    public Result<List<UserResponse>> getUsersWithDepartments(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        List<UserResponse> users = userService.getUsersWithDepartments(keyword);
        return Result.success(users);
    }
}
