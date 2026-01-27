package com.school.management.interfaces.rest.user;

import com.school.management.application.user.UserApplicationService;
import com.school.management.application.user.command.CreateUserCommand;
import com.school.management.application.user.command.UpdateUserCommand;
import com.school.management.infrastructure.audit.annotation.OperationLog;
import com.school.management.common.result.Result;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.exception.BusinessException;
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
 * 用户管理 REST API控制器 (DDD架构 V2)
 *
 * 已完成从V1 UserService到DDD UserApplicationService的迁移
 */
@Slf4j
@Tag(name = "User Management V2", description = "用户管理API - DDD架构")
@RestController("userControllerV2")
@RequestMapping("/v2/users")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;

    // ==================== 基础CRUD ====================

    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    @OperationLog(module = "system", type = "create", name = "创建用户")
    public Result<UserDomainResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("V2 创建用户请求: {}", request.getUsername());

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
                .userType(request.getUserType())
                .roleIds(request.getRoleIds())
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
        log.info("V2 更新用户: {}", id);

        UpdateUserCommand command = UpdateUserCommand.builder()
                .realName(request.getRealName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .employeeNo(request.getEmployeeNo())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .idCard(request.getIdCard())
                .orgUnitId(request.getOrgUnitId())
                .roleIds(request.getRoleIds())
                .build();

        User user = userApplicationService.updateUser(id, command);
        return Result.success(UserDomainResponse.fromDomain(user));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @OperationLog(module = "system", type = "delete", name = "删除用户")
    public Result<Void> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("V2 删除用户: {}", id);
        userApplicationService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @OperationLog(module = "system", type = "delete", name = "批量删除用户")
    public Result<Void> deleteUsers(@RequestBody List<Long> ids) {
        log.info("V2 批量删除用户: {}", ids);
        userApplicationService.deleteUsers(ids);
        return Result.success();
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<UserDomainResponse> getUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        User user = userApplicationService.getUser(id)
                .orElseThrow(() -> new BusinessException("用户不存在: " + id));
        return Result.success(UserDomainResponse.fromDomain(user));
    }

    @Operation(summary = "分页查询用户")
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<PageResponse> getUserPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "真实姓名") @RequestParam(required = false) String realName,
            @Parameter(description = "手机号") @RequestParam(required = false) String phone,
            @Parameter(description = "组织单元ID") @RequestParam(required = false) Long orgUnitId,
            @Parameter(description = "状态: 1启用 0禁用") @RequestParam(required = false) Integer status) {

        List<User> users = userApplicationService.getUsersPage(
                pageNum, pageSize, username, realName, phone, orgUnitId, status);
        long total = userApplicationService.countUsers(username, realName, phone, orgUnitId, status);

        List<UserDomainResponse> records = users.stream()
                .map(UserDomainResponse::fromDomain)
                .collect(Collectors.toList());

        PageResponse pageResponse = new PageResponse(records, total, pageSize, pageNum);
        return Result.success(pageResponse);
    }

    // ==================== 状态操作 ====================

    @Operation(summary = "启用用户")
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "启用用户")
    public Result<Void> enableUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("V2 启用用户: {}", id);
        userApplicationService.enableUser(id);
        return Result.success();
    }

    @Operation(summary = "禁用用户")
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "system", type = "update", name = "禁用用户")
    public Result<Void> disableUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("V2 禁用用户: {}", id);
        userApplicationService.disableUser(id);
        return Result.success();
    }

    @Operation(summary = "更新用户状态")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "状态: 1启用 0禁用") @RequestParam Integer status) {
        log.info("V2 更新用户状态: {} -> {}", id, status);
        if (status == 1) {
            userApplicationService.enableUser(id);
        } else {
            userApplicationService.disableUser(id);
        }
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
        String newPassword = userApplicationService.resetPassword(id);
        return Result.success(newPassword);
    }

    // ==================== 角色操作 ====================

    @Operation(summary = "获取用户角色ID列表")
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<Long>> getUserRoles(
            @Parameter(description = "用户ID") @PathVariable Long id) {
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
        log.info("V2 为用户分配角色: {} -> {}", id, roleIds);
        userApplicationService.assignRoles(id, roleIds);
        return Result.success();
    }

    // ==================== 查询操作 ====================

    @Operation(summary = "检查用户名是否存在")
    @GetMapping("/exists")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<Boolean> existsUsername(
            @Parameter(description = "用户名") @RequestParam String username,
            @Parameter(description = "排除的用户ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = userApplicationService.existsUsername(username, excludeId);
        return Result.success(exists);
    }

    @Operation(summary = "获取简单用户列表")
    @GetMapping("/simple")
    public Result<List<SimpleUserResponse>> getSimpleUserList(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        List<User> users = userApplicationService.getSimpleUserList(keyword);
        List<SimpleUserResponse> responses = users.stream()
                .map(SimpleUserResponse::fromDomain)
                .collect(Collectors.toList());
        return Result.success(responses);
    }

    @Operation(summary = "按组织单元获取用户列表")
    @GetMapping("/by-org-unit/{orgUnitId}")
    public Result<List<UserDomainResponse>> getUsersByOrgUnit(
            @Parameter(description = "组织单元ID") @PathVariable Long orgUnitId,
            @Parameter(description = "是否包含子组织单元用户") @RequestParam(defaultValue = "false") Boolean includeChildren,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        log.info("V2 按组织单元获取用户: orgUnitId={}, includeChildren={}, keyword={}",
                orgUnitId, includeChildren, keyword);
        // TODO: includeChildren和keyword过滤待UserApplicationService扩展支持
        List<User> users = userApplicationService.getUsersByOrgUnit(orgUnitId);
        List<UserDomainResponse> responses = users.stream()
                .map(UserDomainResponse::fromDomain)
                .collect(Collectors.toList());
        return Result.success(responses);
    }

    @Operation(summary = "获取带组织单元信息的用户列表")
    @GetMapping("/with-org-units")
    public Result<List<UserDomainResponse>> getUsersWithOrgUnits(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        // 使用简单列表查询，UserDomainResponse已包含orgUnitName
        List<User> users = (keyword != null && !keyword.isEmpty())
                ? userApplicationService.getSimpleUserList(keyword)
                : userApplicationService.getAllUsers();
        List<UserDomainResponse> responses = users.stream()
                .map(UserDomainResponse::fromDomain)
                .collect(Collectors.toList());
        return Result.success(responses);
    }
}
