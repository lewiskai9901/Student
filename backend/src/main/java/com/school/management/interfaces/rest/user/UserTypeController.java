package com.school.management.interfaces.rest.user;

import com.school.management.application.user.UserTypeApplicationService;
import com.school.management.application.user.UserTypeApplicationService.*;
import com.school.management.common.result.Result;
import com.school.management.domain.user.model.entity.UserType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户类型管理控制器
 */
@RestController
@RequestMapping("/v6/user-types")
@Tag(name = "用户类型管理", description = "V6 用户类型配置 API")
public class UserTypeController {

    private final UserTypeApplicationService userTypeService;

    public UserTypeController(UserTypeApplicationService userTypeService) {
        this.userTypeService = userTypeService;
    }

    @GetMapping
    @Operation(summary = "获取所有用户类型")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<UserType>> getAllUserTypes() {
        return Result.success(userTypeService.getAllUserTypes());
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的用户类型")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<UserType>> getEnabledUserTypes() {
        return Result.success(userTypeService.getEnabledUserTypes());
    }

    @GetMapping("/tree")
    @Operation(summary = "获取用户类型树")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<UserTypeTreeNode>> getUserTypeTree() {
        return Result.success(userTypeService.getUserTypeTree());
    }

    @GetMapping("/loginable")
    @Operation(summary = "获取可登录的类型")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<UserType>> getLoginableTypes() {
        return Result.success(userTypeService.getLoginableTypes());
    }

    @GetMapping("/inspector")
    @Operation(summary = "获取可作为检查员的类型")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<UserType>> getInspectorTypes() {
        return Result.success(userTypeService.getInspectorTypes());
    }

    @GetMapping("/inspectable")
    @Operation(summary = "获取可被检查的类型")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<UserType>> getInspectableTypes() {
        return Result.success(userTypeService.getInspectableTypes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户类型详情")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<UserType> getUserTypeById(@PathVariable Long id) {
        return Result.success(userTypeService.getUserTypeById(id));
    }

    @GetMapping("/code/{typeCode}")
    @Operation(summary = "根据编码获取用户类型")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<UserType> getUserTypeByCode(@PathVariable String typeCode) {
        return Result.success(userTypeService.getUserTypeByCode(typeCode));
    }

    @PostMapping
    @Operation(summary = "创建用户类型")
    @PreAuthorize("hasAuthority('system:user:create')")
    public Result<UserType> createUserType(@RequestBody CreateUserTypeRequest request) {
        CreateUserTypeCommand command = new CreateUserTypeCommand();
        command.setTypeCode(request.getTypeCode());
        command.setTypeName(request.getTypeName());
        command.setParentTypeCode(request.getParentTypeCode());
        command.setLevelOrder(request.getLevelOrder());
        command.setIcon(request.getIcon());
        command.setColor(request.getColor());
        command.setDescription(request.getDescription());
        command.setCanLogin(request.isCanLogin());
        command.setCanBeInspector(request.isCanBeInspector());
        command.setCanBeInspected(request.isCanBeInspected());
        command.setCanManageOrg(request.isCanManageOrg());
        command.setCanViewReports(request.isCanViewReports());
        command.setRequiresClass(request.isRequiresClass());
        command.setRequiresDormitory(request.isRequiresDormitory());
        command.setDefaultRoleCodes(request.getDefaultRoleCodes());
        command.setSortOrder(request.getSortOrder());

        return Result.success(userTypeService.createUserType(command));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户类型")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<UserType> updateUserType(@PathVariable Long id, @RequestBody UpdateUserTypeRequest request) {
        UpdateUserTypeCommand command = new UpdateUserTypeCommand();
        command.setTypeName(request.getTypeName());
        command.setIcon(request.getIcon());
        command.setColor(request.getColor());
        command.setDescription(request.getDescription());
        command.setCanLogin(request.isCanLogin());
        command.setCanBeInspector(request.isCanBeInspector());
        command.setCanBeInspected(request.isCanBeInspected());
        command.setCanManageOrg(request.isCanManageOrg());
        command.setCanViewReports(request.isCanViewReports());
        command.setRequiresClass(request.isRequiresClass());
        command.setRequiresDormitory(request.isRequiresDormitory());
        command.setDefaultRoleCodes(request.getDefaultRoleCodes());
        command.setSortOrder(request.getSortOrder());

        return Result.success(userTypeService.updateUserType(id, command));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户类型")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Result<Void> deleteUserType(@PathVariable Long id) {
        userTypeService.deleteUserType(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用用户类型")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<UserType> enableUserType(@PathVariable Long id) {
        return Result.success(userTypeService.toggleStatus(id, true));
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用用户类型")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<UserType> disableUserType(@PathVariable Long id) {
        return Result.success(userTypeService.toggleStatus(id, false));
    }

    // ==================== 请求对象 ====================

    public static class CreateUserTypeRequest {
        private String typeCode;
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder = 0;
        private String icon;
        private String color;
        private String description;
        private boolean canLogin = true;
        private boolean canBeInspector;
        private boolean canBeInspected;
        private boolean canManageOrg;
        private boolean canViewReports;
        private boolean requiresClass;
        private boolean requiresDormitory;
        private String defaultRoleCodes;
        private Integer sortOrder = 0;

        // Getters and Setters
        public String getTypeCode() { return typeCode; }
        public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public String getParentTypeCode() { return parentTypeCode; }
        public void setParentTypeCode(String parentTypeCode) { this.parentTypeCode = parentTypeCode; }
        public Integer getLevelOrder() { return levelOrder; }
        public void setLevelOrder(Integer levelOrder) { this.levelOrder = levelOrder; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isCanLogin() { return canLogin; }
        public void setCanLogin(boolean canLogin) { this.canLogin = canLogin; }
        public boolean isCanBeInspector() { return canBeInspector; }
        public void setCanBeInspector(boolean canBeInspector) { this.canBeInspector = canBeInspector; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public void setCanBeInspected(boolean canBeInspected) { this.canBeInspected = canBeInspected; }
        public boolean isCanManageOrg() { return canManageOrg; }
        public void setCanManageOrg(boolean canManageOrg) { this.canManageOrg = canManageOrg; }
        public boolean isCanViewReports() { return canViewReports; }
        public void setCanViewReports(boolean canViewReports) { this.canViewReports = canViewReports; }
        public boolean isRequiresClass() { return requiresClass; }
        public void setRequiresClass(boolean requiresClass) { this.requiresClass = requiresClass; }
        public boolean isRequiresDormitory() { return requiresDormitory; }
        public void setRequiresDormitory(boolean requiresDormitory) { this.requiresDormitory = requiresDormitory; }
        public String getDefaultRoleCodes() { return defaultRoleCodes; }
        public void setDefaultRoleCodes(String defaultRoleCodes) { this.defaultRoleCodes = defaultRoleCodes; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UpdateUserTypeRequest {
        private String typeName;
        private String icon;
        private String color;
        private String description;
        private boolean canLogin;
        private boolean canBeInspector;
        private boolean canBeInspected;
        private boolean canManageOrg;
        private boolean canViewReports;
        private boolean requiresClass;
        private boolean requiresDormitory;
        private String defaultRoleCodes;
        private Integer sortOrder;

        // Getters and Setters
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isCanLogin() { return canLogin; }
        public void setCanLogin(boolean canLogin) { this.canLogin = canLogin; }
        public boolean isCanBeInspector() { return canBeInspector; }
        public void setCanBeInspector(boolean canBeInspector) { this.canBeInspector = canBeInspector; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public void setCanBeInspected(boolean canBeInspected) { this.canBeInspected = canBeInspected; }
        public boolean isCanManageOrg() { return canManageOrg; }
        public void setCanManageOrg(boolean canManageOrg) { this.canManageOrg = canManageOrg; }
        public boolean isCanViewReports() { return canViewReports; }
        public void setCanViewReports(boolean canViewReports) { this.canViewReports = canViewReports; }
        public boolean isRequiresClass() { return requiresClass; }
        public void setRequiresClass(boolean requiresClass) { this.requiresClass = requiresClass; }
        public boolean isRequiresDormitory() { return requiresDormitory; }
        public void setRequiresDormitory(boolean requiresDormitory) { this.requiresDormitory = requiresDormitory; }
        public String getDefaultRoleCodes() { return defaultRoleCodes; }
        public void setDefaultRoleCodes(String defaultRoleCodes) { this.defaultRoleCodes = defaultRoleCodes; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}
