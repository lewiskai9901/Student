package com.school.management.interfaces.rest.organization;

import com.school.management.application.organization.OrgUnitTypeApplicationService;
import com.school.management.application.organization.OrgUnitTypeApplicationService.*;
import com.school.management.common.result.Result;
import com.school.management.domain.organization.model.entity.OrgUnitTypeEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织类型管理控制器
 */
@RestController
@RequestMapping("/org-unit-types")
@Tag(name = "组织类型管理", description = "组织类型配置 API")
public class OrgUnitTypeController {

    private final OrgUnitTypeApplicationService orgUnitTypeService;

    public OrgUnitTypeController(OrgUnitTypeApplicationService orgUnitTypeService) {
        this.orgUnitTypeService = orgUnitTypeService;
    }

    @GetMapping
    @Operation(summary = "获取所有组织类型")
    @PreAuthorize("hasAuthority('system:org:view')")
    public Result<List<OrgUnitTypeEntity>> getAllOrgUnitTypes() {
        return Result.success(orgUnitTypeService.getAllOrgUnitTypes());
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的组织类型")
    @PreAuthorize("hasAuthority('system:org:view')")
    public Result<List<OrgUnitTypeEntity>> getEnabledOrgUnitTypes() {
        return Result.success(orgUnitTypeService.getEnabledOrgUnitTypes());
    }

    @GetMapping("/tree")
    @Operation(summary = "获取组织类型树")
    @PreAuthorize("hasAuthority('system:org:view')")
    public Result<List<OrgUnitTypeTreeNode>> getOrgUnitTypeTree() {
        return Result.success(orgUnitTypeService.getOrgUnitTypeTree());
    }

    @GetMapping("/academic")
    @Operation(summary = "获取教学单位类型")
    @PreAuthorize("hasAuthority('system:org:view')")
    public Result<List<OrgUnitTypeEntity>> getAcademicTypes() {
        return Result.success(orgUnitTypeService.getAcademicTypes());
    }

    @GetMapping("/functional")
    @Operation(summary = "获取职能部门类型")
    @PreAuthorize("hasAuthority('system:org:view')")
    public Result<List<OrgUnitTypeEntity>> getFunctionalTypes() {
        return Result.success(orgUnitTypeService.getFunctionalTypes());
    }

    @GetMapping("/inspectable")
    @Operation(summary = "获取可检查的类型")
    @PreAuthorize("hasAuthority('system:org:view')")
    public Result<List<OrgUnitTypeEntity>> getInspectableTypes() {
        return Result.success(orgUnitTypeService.getInspectableTypes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取组织类型详情")
    @PreAuthorize("hasAuthority('system:org:view')")
    public Result<OrgUnitTypeEntity> getOrgUnitTypeById(@PathVariable Long id) {
        return Result.success(orgUnitTypeService.getOrgUnitTypeById(id));
    }

    @GetMapping("/code/{typeCode}")
    @Operation(summary = "根据编码获取组织类型")
    @PreAuthorize("hasAuthority('system:org:view')")
    public Result<OrgUnitTypeEntity> getOrgUnitTypeByCode(@PathVariable String typeCode) {
        return Result.success(orgUnitTypeService.getOrgUnitTypeByCode(typeCode));
    }

    @PostMapping
    @Operation(summary = "创建组织类型")
    @PreAuthorize("hasAuthority('system:org:create')")
    public Result<OrgUnitTypeEntity> createOrgUnitType(@RequestBody CreateOrgUnitTypeRequest request) {
        CreateOrgUnitTypeCommand command = new CreateOrgUnitTypeCommand();
        command.setTypeCode(request.getTypeCode());
        command.setTypeName(request.getTypeName());
        command.setParentTypeCode(request.getParentTypeCode());
        command.setLevelOrder(request.getLevelOrder());
        command.setIcon(request.getIcon());
        command.setColor(request.getColor());
        command.setDescription(request.getDescription());
        command.setAcademic(request.isAcademic());
        command.setCanBeInspected(request.isCanBeInspected());
        command.setCanHaveChildren(request.isCanHaveChildren());
        command.setMaxDepth(request.getMaxDepth());
        command.setSortOrder(request.getSortOrder());

        return Result.success(orgUnitTypeService.createOrgUnitType(command));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新组织类型")
    @PreAuthorize("hasAuthority('system:org:update')")
    public Result<OrgUnitTypeEntity> updateOrgUnitType(@PathVariable Long id, @RequestBody UpdateOrgUnitTypeRequest request) {
        UpdateOrgUnitTypeCommand command = new UpdateOrgUnitTypeCommand();
        command.setTypeName(request.getTypeName());
        command.setIcon(request.getIcon());
        command.setColor(request.getColor());
        command.setDescription(request.getDescription());
        command.setAcademic(request.isAcademic());
        command.setCanBeInspected(request.isCanBeInspected());
        command.setCanHaveChildren(request.isCanHaveChildren());
        command.setMaxDepth(request.getMaxDepth());
        command.setSortOrder(request.getSortOrder());

        return Result.success(orgUnitTypeService.updateOrgUnitType(id, command));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除组织类型")
    @PreAuthorize("hasAuthority('system:org:delete')")
    public Result<Void> deleteOrgUnitType(@PathVariable Long id) {
        orgUnitTypeService.deleteOrgUnitType(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用组织类型")
    @PreAuthorize("hasAuthority('system:org:update')")
    public Result<OrgUnitTypeEntity> enableOrgUnitType(@PathVariable Long id) {
        return Result.success(orgUnitTypeService.toggleStatus(id, true));
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用组织类型")
    @PreAuthorize("hasAuthority('system:org:update')")
    public Result<OrgUnitTypeEntity> disableOrgUnitType(@PathVariable Long id) {
        return Result.success(orgUnitTypeService.toggleStatus(id, false));
    }

    // ==================== 请求对象 ====================

    public static class CreateOrgUnitTypeRequest {
        private String typeCode;
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder = 0;
        private String icon;
        private String color;
        private String description;
        private boolean isAcademic = true;
        private boolean canBeInspected = true;
        private boolean canHaveChildren = true;
        private Integer maxDepth;
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
        public boolean isAcademic() { return isAcademic; }
        public void setAcademic(boolean academic) { isAcademic = academic; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public void setCanBeInspected(boolean canBeInspected) { this.canBeInspected = canBeInspected; }
        public boolean isCanHaveChildren() { return canHaveChildren; }
        public void setCanHaveChildren(boolean canHaveChildren) { this.canHaveChildren = canHaveChildren; }
        public Integer getMaxDepth() { return maxDepth; }
        public void setMaxDepth(Integer maxDepth) { this.maxDepth = maxDepth; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UpdateOrgUnitTypeRequest {
        private String typeName;
        private String icon;
        private String color;
        private String description;
        private boolean isAcademic;
        private boolean canBeInspected;
        private boolean canHaveChildren;
        private Integer maxDepth;
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
        public boolean isAcademic() { return isAcademic; }
        public void setAcademic(boolean academic) { isAcademic = academic; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public void setCanBeInspected(boolean canBeInspected) { this.canBeInspected = canBeInspected; }
        public boolean isCanHaveChildren() { return canHaveChildren; }
        public void setCanHaveChildren(boolean canHaveChildren) { this.canHaveChildren = canHaveChildren; }
        public Integer getMaxDepth() { return maxDepth; }
        public void setMaxDepth(Integer maxDepth) { this.maxDepth = maxDepth; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}
