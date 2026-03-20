package com.school.management.interfaces.rest.user;

import com.school.management.application.shared.TypeTreeBuilder.TypeTreeNode;
import com.school.management.application.user.UserTypeApplicationService;
import com.school.management.application.user.UserTypeApplicationService.*;
import com.school.management.common.result.Result;
import com.school.management.domain.user.model.entity.UserType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户类型管理控制器（统一类型系统 Phase 2）
 * API path: /user-types (was /v6/user-types)
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/user-types")
@Tag(name = "用户类型管理", description = "统一用户类型配置 API")
public class UserTypeController {

    private final UserTypeApplicationService userTypeService;

    @GetMapping("/categories")
    @Operation(summary = "获取所有用户类型分类")
    @CasbinAccess(resource = "system:user", action = "view")
    public Result<List<UserCategoryDTO>> getCategories() {
        return Result.success(userTypeService.getCategories());
    }

    @GetMapping
    @Operation(summary = "获取所有用户类型")
    @CasbinAccess(resource = "system:user", action = "view")
    public Result<List<UserType>> getAllUserTypes() {
        return Result.success(userTypeService.getAllUserTypes());
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的用户类型")
    @CasbinAccess(resource = "system:user", action = "view")
    public Result<List<UserType>> getEnabledUserTypes() {
        return Result.success(userTypeService.getEnabledUserTypes());
    }

    @GetMapping("/tree")
    @Operation(summary = "获取用户类型树")
    @CasbinAccess(resource = "system:user", action = "view")
    public Result<List<TypeTreeNode<UserType>>> getUserTypeTree() {
        return Result.success(userTypeService.getUserTypeTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户类型详情")
    @CasbinAccess(resource = "system:user", action = "view")
    public Result<UserType> getUserTypeById(@PathVariable Long id) {
        return Result.success(userTypeService.getUserTypeById(id));
    }

    @GetMapping("/code/{typeCode}")
    @Operation(summary = "根据编码获取用户类型")
    @CasbinAccess(resource = "system:user", action = "view")
    public Result<UserType> getUserTypeByCode(@PathVariable String typeCode) {
        return Result.success(userTypeService.getUserTypeByCode(typeCode));
    }

    @PostMapping
    @Operation(summary = "创建用户类型")
    @CasbinAccess(resource = "system:user", action = "add")
    public Result<UserType> createUserType(@RequestBody CreateUserTypeRequest request) {
        CreateUserTypeCommand command = CreateUserTypeCommand.builder()
                .typeName(request.getTypeName())
                .category(request.getCategory())
                .parentTypeCode(request.getParentTypeCode())
                .icon(request.getIcon())
                .description(request.getDescription())
                .features(request.getFeatures())
                .metadataSchema(request.getMetadataSchema())
                .allowedChildTypeCodes(request.getAllowedChildTypeCodes())
                .maxDepth(request.getMaxDepth())
                .defaultRoleCodes(request.getDefaultRoleCodes())
                .defaultOrgTypeCodes(request.getDefaultOrgTypeCodes())
                .defaultPlaceTypeCodes(request.getDefaultPlaceTypeCodes())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return Result.success(userTypeService.createUserType(command));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户类型")
    @CasbinAccess(resource = "system:user", action = "edit")
    public Result<UserType> updateUserType(@PathVariable Long id, @RequestBody UpdateUserTypeRequest request) {
        UpdateUserTypeCommand command = UpdateUserTypeCommand.builder()
                .typeName(request.getTypeName())
                .category(request.getCategory())
                .icon(request.getIcon())
                .description(request.getDescription())
                .features(request.getFeatures())
                .metadataSchema(request.getMetadataSchema())
                .allowedChildTypeCodes(request.getAllowedChildTypeCodes())
                .maxDepth(request.getMaxDepth())
                .defaultRoleCodes(request.getDefaultRoleCodes())
                .defaultOrgTypeCodes(request.getDefaultOrgTypeCodes())
                .defaultPlaceTypeCodes(request.getDefaultPlaceTypeCodes())
                .sortOrder(request.getSortOrder())
                .build();
        return Result.success(userTypeService.updateUserType(id, command));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户类型")
    @CasbinAccess(resource = "system:user", action = "delete")
    public Result<Void> deleteUserType(@PathVariable Long id) {
        userTypeService.deleteUserType(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用用户类型")
    @CasbinAccess(resource = "system:user", action = "edit")
    public Result<UserType> enableUserType(@PathVariable Long id) {
        return Result.success(userTypeService.toggleStatus(id, true));
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用用户类型")
    @CasbinAccess(resource = "system:user", action = "edit")
    public Result<UserType> disableUserType(@PathVariable Long id) {
        return Result.success(userTypeService.toggleStatus(id, false));
    }

    // ==================== Request DTOs ====================

    @Data
    public static class CreateUserTypeRequest {
        private String typeName;
        private String category;
        private String parentTypeCode;
        private String icon;
        private String description;
        private Map<String, Boolean> features;
        private String metadataSchema;
        private List<String> allowedChildTypeCodes;
        private Integer maxDepth;
        private List<String> defaultRoleCodes;
        private List<String> defaultOrgTypeCodes;
        private List<String> defaultPlaceTypeCodes;
        private Integer sortOrder;
    }

    @Data
    public static class UpdateUserTypeRequest {
        private String typeName;
        private String category;
        private String icon;
        private String description;
        private Map<String, Boolean> features;
        private String metadataSchema;
        private List<String> allowedChildTypeCodes;
        private Integer maxDepth;
        private List<String> defaultRoleCodes;
        private List<String> defaultOrgTypeCodes;
        private List<String> defaultPlaceTypeCodes;
        private Integer sortOrder;
    }
}
