package com.school.management.casbin.controller;

import com.school.management.casbin.dto.PermissionCheckDTO;
import com.school.management.casbin.dto.ScopeAssignmentDTO;
import com.school.management.casbin.dto.ScopeMetadataDTO;
import com.school.management.casbin.enums.ScopeType;
import com.school.management.casbin.service.CasbinEnforcerService;
import com.school.management.casbin.service.CasbinScopeService;
import com.school.management.common.ApiResponse;
import com.school.management.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据范围管理控制器
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/v2/scopes")
@RequiredArgsConstructor
@Tag(name = "数据范围管理", description = "基于Casbin的数据范围管理API")
@ConditionalOnProperty(name = "casbin.enabled", havingValue = "true", matchIfMissing = false)
public class ScopeManageController {

    private final CasbinScopeService scopeService;
    private final CasbinEnforcerService enforcerService;

    // ==================== 范围分配 ====================

    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('scope:assign')")
    @Operation(summary = "分配用户数据范围")
    public ApiResponse<ScopeAssignmentDTO> assignScope(@Validated @RequestBody ScopeAssignmentDTO dto) {
        ScopeAssignmentDTO result = scopeService.assignScope(dto);
        return ApiResponse.success(result);
    }

    @PostMapping("/batch-assign")
    @PreAuthorize("hasAuthority('scope:assign')")
    @Operation(summary = "批量分配用户数据范围")
    public ApiResponse<List<ScopeAssignmentDTO>> batchAssignScopes(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "范围表达式列表") @RequestBody List<String> scopes) {
        List<ScopeAssignmentDTO> results = scopeService.batchAssignScopes(userId, scopes);
        return ApiResponse.success(results);
    }

    @DeleteMapping("/revoke")
    @PreAuthorize("hasAuthority('scope:assign')")
    @Operation(summary = "移除用户数据范围")
    public ApiResponse<Void> revokeScope(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "范围表达式") @RequestParam String scopeExpression) {
        scopeService.removeScope(userId, scopeExpression);
        return ApiResponse.success();
    }

    @DeleteMapping("/revoke-all")
    @PreAuthorize("hasAuthority('scope:assign')")
    @Operation(summary = "移除用户所有数据范围")
    public ApiResponse<Void> revokeAllScopes(
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        scopeService.removeAllScopes(userId);
        return ApiResponse.success();
    }

    // ==================== 范围查询 ====================

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('scope:view')")
    @Operation(summary = "获取用户数据范围列表")
    public ApiResponse<List<ScopeAssignmentDTO>> getUserScopes(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        List<ScopeAssignmentDTO> scopes = scopeService.getUserScopes(userId);
        return ApiResponse.success(scopes);
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('scope:view')")
    @Operation(summary = "分页查询用户范围分配")
    public ApiResponse<PageResult<ScopeAssignmentDTO>> pageUserScopes(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<ScopeAssignmentDTO> result = scopeService.pageUserScopes(userId, pageNum, pageSize);
        return ApiResponse.success(result);
    }

    @GetMapping("/by-expression")
    @PreAuthorize("hasAuthority('scope:view')")
    @Operation(summary = "获取拥有指定范围的用户列表")
    public ApiResponse<List<ScopeAssignmentDTO>> getUsersByScope(
            @Parameter(description = "范围表达式") @RequestParam String scopeExpression) {
        List<ScopeAssignmentDTO> users = scopeService.getUsersByScope(scopeExpression);
        return ApiResponse.success(users);
    }

    // ==================== 范围元数据 ====================

    @GetMapping("/metadata")
    @Operation(summary = "获取所有范围元数据")
    public ApiResponse<List<ScopeMetadataDTO>> getAllMetadata() {
        List<ScopeMetadataDTO> metadata = scopeService.getAllMetadata();
        return ApiResponse.success(metadata);
    }

    @GetMapping("/metadata/by-type")
    @Operation(summary = "按类型获取范围元数据")
    public ApiResponse<List<ScopeMetadataDTO>> getMetadataByType(
            @Parameter(description = "范围类型") @RequestParam String scopeType) {
        List<ScopeMetadataDTO> metadata = scopeService.getMetadataByType(scopeType);
        return ApiResponse.success(metadata);
    }

    @GetMapping("/types")
    @Operation(summary = "获取所有范围类型")
    public ApiResponse<List<ScopeTypeVO>> getScopeTypes() {
        List<ScopeTypeVO> types = Arrays.stream(ScopeType.values())
                .map(t -> new ScopeTypeVO(t.getCode(), t.getName()))
                .collect(Collectors.toList());
        return ApiResponse.success(types);
    }

    @PostMapping("/metadata/sync")
    @PreAuthorize("hasAuthority('scope:manage')")
    @Operation(summary = "同步范围元数据")
    public ApiResponse<Void> syncMetadata() {
        scopeService.syncMetadata();
        return ApiResponse.success();
    }

    // ==================== 权限检查 ====================

    @PostMapping("/check")
    @Operation(summary = "检查用户权限")
    public ApiResponse<PermissionCheckDTO> checkPermission(@Validated @RequestBody PermissionCheckDTO dto) {
        boolean allowed;
        if (dto.getScope() != null) {
            allowed = enforcerService.checkPermission(dto.getUserId(), dto.getScope(),
                    dto.getResource(), dto.getAction());
        } else {
            allowed = enforcerService.checkPermission(dto.getUserId(),
                    dto.getResource(), dto.getAction());
        }
        dto.setAllowed(allowed);
        return ApiResponse.success(dto);
    }

    @GetMapping("/accessible-classes")
    @Operation(summary = "获取用户可访问的班级ID列表")
    public ApiResponse<Set<Long>> getAccessibleClassIds(
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        Set<Long> classIds = enforcerService.getAccessibleClassIds(userId);
        return ApiResponse.success(classIds);
    }

    @GetMapping("/accessible-grades")
    @Operation(summary = "获取用户可访问的年级ID列表")
    public ApiResponse<Set<Long>> getAccessibleGradeIds(
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        Set<Long> gradeIds = enforcerService.getAccessibleGradeIds(userId);
        return ApiResponse.success(gradeIds);
    }

    @GetMapping("/accessible-depts")
    @Operation(summary = "获取用户可访问的部门ID列表")
    public ApiResponse<Set<Long>> getAccessibleDeptIds(
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        Set<Long> deptIds = enforcerService.getAccessibleDeptIds(userId);
        return ApiResponse.success(deptIds);
    }

    // ==================== 系统管理 ====================

    @PostMapping("/reload")
    @PreAuthorize("hasAuthority('scope:manage')")
    @Operation(summary = "重新加载策略")
    public ApiResponse<Void> reloadPolicy() {
        enforcerService.reloadPolicy();
        return ApiResponse.success();
    }

    // ==================== 内部类 ====================

    /**
     * 范围类型VO
     */
    public record ScopeTypeVO(String code, String name) {
    }
}
