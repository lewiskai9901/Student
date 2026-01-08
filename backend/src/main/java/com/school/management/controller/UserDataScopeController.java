package com.school.management.controller;

import com.school.management.common.ApiResponse;
import com.school.management.dto.UserDataScopeDTO;
import com.school.management.dto.request.UserDataScopeRequest;
import com.school.management.enums.ScopeType;
import com.school.management.service.UserDataScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 用户数据范围控制器
 *
 * 用于管理用户的数据访问范围
 * 核心设计：角色（功能权限）与数据范围（数据权限）正交分离
 *
 * @author system
 * @version 3.0.0
 */
@Slf4j
@RestController
@RequestMapping("/user-data-scopes")
@RequiredArgsConstructor
@Tag(name = "用户数据范围管理", description = "管理用户的数据访问范围")
public class UserDataScopeController {

    private final UserDataScopeService userDataScopeService;

    // ==================== 基础CRUD ====================

    @PostMapping
    @Operation(summary = "添加用户数据范围")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public ApiResponse<UserDataScopeDTO> addScope(@Valid @RequestBody UserDataScopeRequest request) {
        log.info("添加用户数据范围: userId={}, scopeType={}, scopeId={}",
                request.getUserId(), request.getScopeType(), request.getScopeId());
        UserDataScopeDTO result = userDataScopeService.addScope(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量添加用户数据范围")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public ApiResponse<List<UserDataScopeDTO>> batchAddScopes(@Valid @RequestBody UserDataScopeRequest.BatchAdd request) {
        log.info("批量添加用户数据范围: userId={}, count={}",
                request.getUserId(), request.getScopes().size());
        List<UserDataScopeDTO> result = userDataScopeService.batchAddScopes(request);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户数据范围")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public ApiResponse<Void> deleteScope(
            @Parameter(description = "数据范围ID") @PathVariable Long id) {
        log.info("删除用户数据范围: id={}", id);
        userDataScopeService.deleteScope(id);
        return ApiResponse.success();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除用户数据范围")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public ApiResponse<Void> batchDeleteScopes(@RequestBody UserDataScopeRequest.BatchDelete request) {
        log.info("批量删除用户数据范围: ids={}", request.getIds());
        userDataScopeService.batchDeleteScopes(request.getIds());
        return ApiResponse.success();
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "删除用户所有数据范围")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public ApiResponse<Void> deleteAllByUserId(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("删除用户所有数据范围: userId={}", userId);
        userDataScopeService.deleteAllByUserId(userId);
        return ApiResponse.success();
    }

    // ==================== 查询接口 ====================

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户的数据范围列表")
    @PreAuthorize("hasAuthority('system:user:view')")
    public ApiResponse<List<UserDataScopeDTO>> getScopesByUserId(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        List<UserDataScopeDTO> result = userDataScopeService.getScopesByUserId(userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/scope")
    @Operation(summary = "获取拥有指定范围的用户列表")
    @PreAuthorize("hasAuthority('system:user:view')")
    public ApiResponse<List<UserDataScopeDTO>> getUsersByScopeTypeAndId(
            @Parameter(description = "范围类型: DEPARTMENT/GRADE/CLASS") @RequestParam String scopeType,
            @Parameter(description = "范围ID") @RequestParam Long scopeId) {
        ScopeType type = ScopeType.fromCode(scopeType);
        if (type == null) {
            return ApiResponse.error("无效的范围类型");
        }
        List<UserDataScopeDTO> result = userDataScopeService.getUsersByScopeTypeAndId(type, scopeId);
        return ApiResponse.success(result);
    }

    @GetMapping("/check")
    @Operation(summary = "检查用户是否有指定范围的权限")
    @PreAuthorize("hasAuthority('system:user:view')")
    public ApiResponse<Boolean> hasScope(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "范围类型") @RequestParam String scopeType,
            @Parameter(description = "范围ID") @RequestParam Long scopeId) {
        ScopeType type = ScopeType.fromCode(scopeType);
        if (type == null) {
            return ApiResponse.error("无效的范围类型");
        }
        boolean result = userDataScopeService.hasScope(userId, type, scopeId);
        return ApiResponse.success(result);
    }

    // ==================== 权限查询接口 ====================

    @GetMapping("/accessible/departments/{userId}")
    @Operation(summary = "获取用户可访问的部门ID列表")
    @PreAuthorize("hasAuthority('system:user:view')")
    public ApiResponse<Set<Long>> getAccessibleDeptIds(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        Set<Long> result = userDataScopeService.getAccessibleDeptIds(userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/accessible/grades/{userId}")
    @Operation(summary = "获取用户可访问的年级ID列表")
    @PreAuthorize("hasAuthority('system:user:view')")
    public ApiResponse<Set<Long>> getAccessibleGradeIds(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        Set<Long> result = userDataScopeService.getAccessibleGradeIds(userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/accessible/classes/{userId}")
    @Operation(summary = "获取用户可访问的班级ID列表")
    @PreAuthorize("hasAuthority('system:user:view')")
    public ApiResponse<Set<Long>> getAccessibleClassIds(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        Set<Long> result = userDataScopeService.getAccessibleClassIds(userId);
        return ApiResponse.success(result);
    }

    // ==================== 范围类型元数据 ====================

    @GetMapping("/scope-types")
    @Operation(summary = "获取所有范围类型")
    public ApiResponse<List<ScopeTypeVO>> getScopeTypes() {
        List<ScopeTypeVO> result = new java.util.ArrayList<>();
        for (ScopeType type : ScopeType.values()) {
            result.add(new ScopeTypeVO(type.getCode(), type.getName(), type.getLevel()));
        }
        return ApiResponse.success(result);
    }

    /**
     * 范围类型VO
     */
    public record ScopeTypeVO(String code, String name, int level) {}
}
