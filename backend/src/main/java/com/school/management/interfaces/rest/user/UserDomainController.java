package com.school.management.interfaces.rest.user;

import com.school.management.infrastructure.activity.annotation.AuditEvent;
import com.school.management.application.access.AccessApplicationService;
import com.school.management.application.user.UserApplicationService;
import com.school.management.application.user.command.CreateUserCommand;
import com.school.management.application.user.command.UpdateUserCommand;
import com.school.management.common.result.Result;
import com.school.management.domain.access.model.Role;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.common.util.SecurityUtils;
import com.school.management.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.school.management.infrastructure.casbin.CasbinAccess;
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
@RestController("userDomainController")
@RequestMapping("/domain/users")
@RequiredArgsConstructor
public class UserDomainController {

    private final UserApplicationService userApplicationService;
    private final AccessApplicationService accessApplicationService;

    // ==================== 创建与更新 ====================

    @Operation(summary = "创建用户")
    @PostMapping
    @CasbinAccess(resource = "system:user", action = "add")
    @AuditEvent(module = "system", action = "CREATE", resourceType = "USER", label = "创建用户")
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
                .orgUnitId(request.getOrgUnitId())
                .placeId(request.getPlaceId())
                .userTypeCode(request.getUserTypeCode())
                .roleIds(request.getRoleIds())
                .createdBy(SecurityUtils.requireCurrentUserId())
                .build();

        User user = userApplicationService.createUser(command);
        return Result.success(UserDomainResponse.fromDomain(user));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "system:user", action = "edit")
    @AuditEvent(module = "system", action = "UPDATE", resourceType = "USER", resourceId = "#id", label = "更新用户")
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
                .orgUnitId(request.getOrgUnitId())
                .userTypeCode(request.getUserTypeCode())
                .roleIds(request.getRoleIds())
                .updatedBy(SecurityUtils.requireCurrentUserId())
                .build();

        User user = userApplicationService.updateUser(id, command);
        return Result.success(UserDomainResponse.fromDomain(user));
    }

    // ==================== 删除操作 ====================

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "system:user", action = "delete")
    @AuditEvent(module = "system", action = "DELETE", resourceType = "USER", resourceId = "#id", label = "删除用户")
    public Result<Void> deleteUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("DDD 删除用户: {}", id);
        userApplicationService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/batch")
    @CasbinAccess(resource = "system:user", action = "delete")
    @AuditEvent(module = "system", action = "DELETE", resourceType = "USER", label = "批量删除用户")
    public Result<Void> deleteUsers(@RequestBody List<Long> ids) {
        log.info("DDD 批量删除用户: {}", ids);
        userApplicationService.deleteUsers(ids);
        return Result.success();
    }

    // ==================== 查询操作 ====================

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @CasbinAccess(resource = "system:user", action = "view")
    public Result<PageResponse> getUsersPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "姓名") @RequestParam(required = false) String realName,
            @Parameter(description = "手机号") @RequestParam(required = false) String phone,
            @Parameter(description = "组织单元ID") @RequestParam(required = false) Long orgUnitId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {

        List<UserDomainResponse> users = userApplicationService
                .getUsersPage(pageNum, pageSize, username, realName, phone, orgUnitId, status)
                .stream()
                .map(UserDomainResponse::fromDomain)
                .collect(Collectors.toList());

        long total = userApplicationService.countUsers(username, realName, phone, orgUnitId, status);

        return Result.success(new PageResponse(users, total, pageSize, pageNum));
    }

    @Operation(summary = "获取所有用户")
    @GetMapping
    @CasbinAccess(resource = "system:user", action = "view")
    public Result<List<UserDomainResponse>> getAllUsers() {
        List<UserDomainResponse> users = userApplicationService.getAllUsers()
                .stream()
                .map(UserDomainResponse::fromDomain)
                .collect(Collectors.toList());
        return Result.success(users);
    }

    @Operation(summary = "获取简单用户列表（用于选择器）")
    @GetMapping("/simple")
    @CasbinAccess(resource = "system:user", action = "view")
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
    @CasbinAccess(resource = "system:user", action = "view")
    public Result<UserDomainResponse> getUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        return userApplicationService.getUser(id)
                .map(user -> Result.success(UserDomainResponse.fromDomain(user)))
                .orElse(Result.error("用户不存在"));
    }

    @Operation(summary = "根据用户名获取用户")
    @GetMapping("/by-username/{username}")
    @CasbinAccess(resource = "system:user", action = "view")
    public Result<UserDomainResponse> getUserByUsername(
            @Parameter(description = "用户名") @PathVariable String username) {
        return userApplicationService.getUserByUsername(username)
                .map(user -> Result.success(UserDomainResponse.fromDomain(user)))
                .orElse(Result.error("用户不存在"));
    }

    @Operation(summary = "根据组织单元获取用户列表")
    @GetMapping("/by-org-unit/{orgUnitId}")
    @CasbinAccess(resource = "system:user", action = "view")
    public Result<List<UserDomainResponse>> getUsersByOrgUnit(
            @Parameter(description = "组织单元ID") @PathVariable Long orgUnitId) {
        List<UserDomainResponse> users = userApplicationService.getUsersByOrgUnit(orgUnitId)
                .stream()
                .map(UserDomainResponse::fromDomain)
                .collect(Collectors.toList());
        return Result.success(users);
    }

    @Operation(summary = "检查用户名是否存在")
    @GetMapping("/exists")
    @CasbinAccess(resource = "system:user", action = "view")
    public Result<Boolean> existsUsername(
            @Parameter(description = "用户名") @RequestParam String username,
            @Parameter(description = "排除的用户ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = userApplicationService.existsUsername(username, excludeId);
        return Result.success(exists);
    }

    // ==================== 状态操作 ====================

    @Operation(summary = "启用用户")
    @PostMapping("/{id}/enable")
    @CasbinAccess(resource = "system:user", action = "edit")
    @AuditEvent(module = "system", action = "UPDATE", resourceType = "USER", resourceId = "#id", label = "启用用户")
    public Result<UserDomainResponse> enableUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("DDD 启用用户: {}", id);
        User user = userApplicationService.enableUser(id);
        return Result.success(UserDomainResponse.fromDomain(user));
    }

    @Operation(summary = "禁用用户")
    @PostMapping("/{id}/disable")
    @CasbinAccess(resource = "system:user", action = "edit")
    @AuditEvent(module = "system", action = "UPDATE", resourceType = "USER", resourceId = "#id", label = "禁用用户")
    public Result<UserDomainResponse> disableUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("DDD 禁用用户: {}", id);
        User user = userApplicationService.disableUser(id);
        return Result.success(UserDomainResponse.fromDomain(user));
    }

    // ==================== 密码操作 ====================

    @Operation(summary = "重置用户密码")
    @PostMapping("/{id}/reset-password")
    @CasbinAccess(resource = "system:user", action = "edit")
    @AuditEvent(module = "system", action = "UPDATE", resourceType = "USER", resourceId = "#id", label = "重置用户密码")
    public Result<String> resetPassword(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("DDD 重置用户密码: {}", id);

        // 检查目标用户是否拥有管理员角色，若有则仅超级管理员可重置
        List<Role> targetRoles = accessApplicationService.getUserRoles(id);
        boolean targetIsAdmin = targetRoles.stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getRoleType())
                        || (r.getRoleCode() != null && r.getRoleCode().toUpperCase().contains("ADMIN")));
        if (targetIsAdmin) {
            Long currentUserId = SecurityUtils.requireCurrentUserId();
            List<Role> currentRoles = accessApplicationService.getUserRoles(currentUserId);
            boolean isSuperAdmin = currentRoles.stream()
                    .anyMatch(r -> "SUPER_ADMIN".equalsIgnoreCase(r.getRoleType())
                            || (r.getRoleCode() != null && r.getRoleCode().toUpperCase().contains("SUPER_ADMIN")));
            if (!isSuperAdmin) {
                throw new BusinessException("仅超级管理员可重置管理员用户的密码");
            }
        }

        userApplicationService.resetPassword(id);
        return Result.success("密码已重置成功");
    }

    // ==================== 角色操作 ====================

    @Operation(summary = "获取用户角色ID列表")
    @GetMapping("/{id}/roles")
    @CasbinAccess(resource = "system:user", action = "view")
    public Result<List<Long>> getUserRoleIds(@Parameter(description = "用户ID") @PathVariable Long id) {
        List<Long> roleIds = userApplicationService.getUserRoleIds(id);
        return Result.success(roleIds);
    }

    @Operation(summary = "分配角色给用户")
    @PostMapping("/{id}/roles")
    @CasbinAccess(resource = "system:user", action = "edit")
    @AuditEvent(module = "system", action = "UPDATE", resourceType = "USER", resourceId = "#id", label = "分配用户角色")
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
    @CasbinAccess(resource = "system:user", action = "edit")
    public Result<Void> bindWechat(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestParam String openid) {
        log.info("DDD 绑定微信: userId={}, openid={}", id, openid);
        userApplicationService.bindWechat(id, openid);
        return Result.success();
    }

    @Operation(summary = "解绑微信")
    @PostMapping("/{id}/unbind-wechat")
    @CasbinAccess(resource = "system:user", action = "edit")
    public Result<Void> unbindWechat(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("DDD 解绑微信: {}", id);
        userApplicationService.unbindWechat(id);
        return Result.success();
    }

}
