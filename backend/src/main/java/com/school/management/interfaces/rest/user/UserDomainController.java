package com.school.management.interfaces.rest.user;

import com.school.management.annotation.OperationLog;
import com.school.management.application.user.UserApplicationService;
import com.school.management.application.user.command.CreateUserCommand;
import com.school.management.application.user.command.UpdateUserCommand;
import com.school.management.common.result.Result;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理 REST API控制器 (纯 DDD 架构)
 *
 * 使用 DDD 应用服务实现，完全脱离 V1 架构
 */
@Slf4j
@Tag(name = "User Domain API", description = "用户管理API - 纯DDD架构")
@RestController("userDomainControllerV2")
@RequestMapping("/v2/domain/users")
@RequiredArgsConstructor
public class UserDomainController {

    private final UserApplicationService userApplicationService;
    private final JwtTokenService jwtTokenService;

    // ==================== 创建与更新 ====================

    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    @OperationLog(module = "system", type = "create", name = "创建用户")
    public Result<UserDomainResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("DDD 创建用户: {}", request.getUsername());

        CreateUserCommand command = CreateUserCommand.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .realName(request.getRealName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .employeeNo(request.getEmployeeNo())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .idCard(request.getIdCard())
                .departmentId(request.getDepartmentId())
                .userType(request.getUserType())
                .roleIds(request.getRoleIds())
                .createdBy(getCurrentUserId())
                .build();

        User user = userApplicationService.createUser(command);
        return Result.success(UserDomainResponse.fromDomain(user));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "更新用户")
    public Result<UserDomainResponse> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("DDD 更新用户: {}", id);

        UpdateUserCommand command = UpdateUserCommand.builder()
                .realName(request.getRealName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .employeeNo(request.getEmployeeNo())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .idCard(request.getIdCard())
                .departmentId(request.getDepartmentId())
                .roleIds(request.getRoleIds())
                .updatedBy(getCurrentUserId())
                .build();

        User user = userApplicationService.updateUser(id, command);
        return Result.success(UserDomainResponse.fromDomain(user));
    }

    // ==================== 删除操作 ====================

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @OperationLog(module = "system", type = "delete", name = "删除用户")
    public Result<Void> deleteUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("DDD 删除用户: {}", id);
        userApplicationService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @OperationLog(module = "system", type = "delete", name = "批量删除用户")
    public Result<Void> deleteUsers(@RequestBody List<Long> ids) {
        log.info("DDD 批量删除用户: {}", ids);
        userApplicationService.deleteUsers(ids);
        return Result.success();
    }

    // ==================== 查询操作 ====================

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<PageResponse> getUsersPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "姓名") @RequestParam(required = false) String realName,
            @Parameter(description = "手机号") @RequestParam(required = false) String phone,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {

        List<UserDomainResponse> users = userApplicationService
                .getUsersPage(pageNum, pageSize, username, realName, phone, departmentId, status)
                .stream()
                .map(UserDomainResponse::fromDomain)
                .collect(Collectors.toList());

        long total = userApplicationService.countUsers(username, realName, phone, departmentId, status);

        return Result.success(new PageResponse(users, total, pageSize, pageNum));
    }

    @Operation(summary = "获取所有用户")
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<UserDomainResponse>> getAllUsers() {
        List<UserDomainResponse> users = userApplicationService.getAllUsers()
                .stream()
                .map(UserDomainResponse::fromDomain)
                .collect(Collectors.toList());
        return Result.success(users);
    }

    @Operation(summary = "获取简单用户列表（用于选择器）")
    @GetMapping("/simple")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<SimpleUserResponse>> getSimpleUserList(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        List<SimpleUserResponse> users = userApplicationService.getSimpleUserList(keyword)
                .stream()
                .map(SimpleUserResponse::fromDomain)
                .collect(Collectors.toList());
        return Result.success(users);
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<UserDomainResponse> getUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        return userApplicationService.getUser(id)
                .map(user -> Result.success(UserDomainResponse.fromDomain(user)))
                .orElse(Result.error("用户不存在"));
    }

    @Operation(summary = "根据用户名获取用户")
    @GetMapping("/by-username/{username}")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<UserDomainResponse> getUserByUsername(
            @Parameter(description = "用户名") @PathVariable String username) {
        return userApplicationService.getUserByUsername(username)
                .map(user -> Result.success(UserDomainResponse.fromDomain(user)))
                .orElse(Result.error("用户不存在"));
    }

    @Operation(summary = "根据部门获取用户列表")
    @GetMapping("/by-department/{departmentId}")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<UserDomainResponse>> getUsersByDepartment(
            @Parameter(description = "部门ID") @PathVariable Long departmentId) {
        List<UserDomainResponse> users = userApplicationService.getUsersByDepartment(departmentId)
                .stream()
                .map(UserDomainResponse::fromDomain)
                .collect(Collectors.toList());
        return Result.success(users);
    }

    @Operation(summary = "检查用户名是否存在")
    @GetMapping("/exists")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<Boolean> existsUsername(
            @Parameter(description = "用户名") @RequestParam String username,
            @Parameter(description = "排除的用户ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = userApplicationService.existsUsername(username, excludeId);
        return Result.success(exists);
    }

    // ==================== 状态操作 ====================

    @Operation(summary = "启用用户")
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "启用用户")
    public Result<UserDomainResponse> enableUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("DDD 启用用户: {}", id);
        User user = userApplicationService.enableUser(id);
        return Result.success(UserDomainResponse.fromDomain(user));
    }

    @Operation(summary = "禁用用户")
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "禁用用户")
    public Result<UserDomainResponse> disableUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("DDD 禁用用户: {}", id);
        User user = userApplicationService.disableUser(id);
        return Result.success(UserDomainResponse.fromDomain(user));
    }

    // ==================== 密码操作 ====================

    @Operation(summary = "重置用户密码")
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "重置用户密码")
    public Result<String> resetPassword(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("DDD 重置用户密码: {}", id);
        String newPassword = userApplicationService.resetPassword(id);
        return Result.success(newPassword);
    }

    // ==================== 角色操作 ====================

    @Operation(summary = "获取用户角色ID列表")
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<Long>> getUserRoleIds(@Parameter(description = "用户ID") @PathVariable Long id) {
        List<Long> roleIds = userApplicationService.getUserRoleIds(id);
        return Result.success(roleIds);
    }

    @Operation(summary = "分配角色给用户")
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "分配用户角色")
    public Result<Void> assignRoles(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody List<Long> roleIds) {
        log.info("DDD 分配用户角色: {} -> {}", id, roleIds);
        userApplicationService.assignRoles(id, roleIds);
        return Result.success();
    }

    // ==================== 微信绑定 ====================

    @Operation(summary = "绑定微信")
    @PostMapping("/{id}/bind-wechat")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> bindWechat(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestParam String openid) {
        log.info("DDD 绑定微信: userId={}, openid={}", id, openid);
        userApplicationService.bindWechat(id, openid);
        return Result.success();
    }

    @Operation(summary = "解绑微信")
    @PostMapping("/{id}/unbind-wechat")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> unbindWechat(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("DDD 解绑微信: {}", id);
        userApplicationService.unbindWechat(id);
        return Result.success();
    }

    // ==================== Helper ====================

    private Long getCurrentUserId() {
        return jwtTokenService.getCurrentUserId();
    }
}
