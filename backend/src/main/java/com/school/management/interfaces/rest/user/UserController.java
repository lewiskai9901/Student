package com.school.management.interfaces.rest.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.annotation.OperationLog;
import com.school.management.common.result.Result;
import com.school.management.dto.UserCreateRequest;
import com.school.management.dto.UserQueryRequest;
import com.school.management.dto.UserResponse;
import com.school.management.dto.UserUpdateRequest;
import com.school.management.service.UserService;
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
 * 用户管理 REST API控制器 (DDD架构 V2)
 *
 * 迁移说明：当前版本使用V1服务实现，后续会迁移到DDD应用服务
 */
@Slf4j
@Tag(name = "User Management V2", description = "用户管理API - DDD架构")
@RestController("userControllerV2")
@RequestMapping("/v2/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService; // 暂时使用V1服务

    // ==================== 基础CRUD ====================

    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    @OperationLog(module = "system", type = "create", name = "创建用户")
    public Result<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("V2 创建用户请求: {}", request.getUsername());
        UserResponse response = userService.createUser(request);
        return Result.success(response);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "更新用户")
    public Result<UserResponse> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("V2 更新用户: {}", id);
        UserResponse response = userService.updateUser(id, request);
        return Result.success(response);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @OperationLog(module = "system", type = "delete", name = "删除用户")
    public Result<Void> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("V2 删除用户: {}", id);
        userService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @OperationLog(module = "system", type = "delete", name = "批量删除用户")
    public Result<Void> deleteUsers(@RequestBody List<Long> ids) {
        log.info("V2 批量删除用户: {}", ids);
        userService.deleteUsers(ids);
        return Result.success();
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<UserResponse> getUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return Result.success(user);
    }

    @Operation(summary = "分页查询用户")
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<Page<UserResponse>> getUserPage(UserQueryRequest request) {
        Page<UserResponse> result = userService.getUserPage(request);
        return Result.success(result);
    }

    // ==================== 状态操作 ====================

    @Operation(summary = "启用用户")
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "启用用户")
    public Result<Void> enableUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("V2 启用用户: {}", id);
        userService.updateUserStatus(id, 1);
        return Result.success();
    }

    @Operation(summary = "禁用用户")
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "禁用用户")
    public Result<Void> disableUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("V2 禁用用户: {}", id);
        userService.updateUserStatus(id, 0);
        return Result.success();
    }

    @Operation(summary = "更新用户状态")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "状态: 1启用 0禁用") @RequestParam Integer status) {
        log.info("V2 更新用户状态: {} -> {}", id, status);
        userService.updateUserStatus(id, status);
        return Result.success();
    }

    // ==================== 密码操作 ====================

    @Operation(summary = "重置用户密码")
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "重置用户密码")
    public Result<String> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("V2 重置用户密码: {}", id);
        String newPassword = userService.resetPassword(id);
        return Result.success(newPassword);
    }

    // ==================== 角色操作 ====================

    @Operation(summary = "获取用户角色ID列表")
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<Long>> getUserRoles(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        List<Long> roleIds = userService.getUserRoleIds(id);
        return Result.success(roleIds);
    }

    @Operation(summary = "分配角色给用户")
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "分配用户角色")
    public Result<Void> assignRoles(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody List<Long> roleIds) {
        log.info("V2 为用户分配角色: {} -> {}", id, roleIds);
        userService.assignRoles(id, roleIds);
        return Result.success();
    }

    // ==================== 查询操作 ====================

    @Operation(summary = "检查用户名是否存在")
    @GetMapping("/exists")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<Boolean> existsUsername(
            @Parameter(description = "用户名") @RequestParam String username,
            @Parameter(description = "排除的用户ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = userService.existsUsername(username, excludeId);
        return Result.success(exists);
    }

    @Operation(summary = "获取简单用户列表")
    @GetMapping("/simple")
    public Result<List<UserResponse>> getSimpleUserList(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        List<UserResponse> users = userService.getSimpleUserList(keyword);
        return Result.success(users);
    }

    @Operation(summary = "按组织单元获取用户列表")
    @GetMapping("/by-org-unit/{orgUnitId}")
    public Result<List<UserResponse>> getUsersByOrgUnit(
            @Parameter(description = "组织单元ID") @PathVariable Long orgUnitId,
            @Parameter(description = "是否包含子组织单元用户") @RequestParam(defaultValue = "false") Boolean includeChildren,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        log.info("V2 按组织单元获取用户: orgUnitId={}, includeChildren={}, keyword={}",
                orgUnitId, includeChildren, keyword);
        List<UserResponse> users = userService.getUsersByOrgUnit(orgUnitId, includeChildren, keyword);
        return Result.success(users);
    }

    @Operation(summary = "获取带组织单元信息的用户列表")
    @GetMapping("/with-org-units")
    public Result<List<UserResponse>> getUsersWithOrgUnits(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        List<UserResponse> users = userService.getUsersWithOrgUnits(keyword);
        return Result.success(users);
    }
}
