package com.school.management.interfaces.rest.organization;

import com.school.management.application.organization.OrgTypeApplicationService;
import com.school.management.application.organization.OrgTypeApplicationService.*;
import com.school.management.common.result.Result;
import com.school.management.domain.organization.model.entity.OrgType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织类型管理控制器
 */
@RestController
@RequestMapping("/v6/org-types")
@Tag(name = "组织类型管理", description = "V6 组织类型配置 API")
public class OrgTypeController {

    private final OrgTypeApplicationService orgTypeService;

    public OrgTypeController(OrgTypeApplicationService orgTypeService) {
        this.orgTypeService = orgTypeService;
    }

    @GetMapping
    @Operation(summary = "获取所有组织类型")
    @PreAuthorize("hasAuthority('system:org:view')")
    public Result<List<OrgType>> getAllOrgTypes() {
        return Result.success(orgTypeService.getAllOrgTypes());
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的组织类型")
    @PreAuthorize("hasAuthority('system:org:view')")
    public Result<List<OrgType>> getEnabledOrgTypes() {
        return Result.success(orgTypeService.getEnabledOrgTypes());
    }

    @GetMapping("/tree")
    @Operation(summary = "获取组织类型树")
    @PreAuthorize("hasAuthority('system:org:view')")
    public Result<List<OrgTypeTreeNode>> getOrgTypeTree() {
        return Result.success(orgTypeService.getOrgTypeTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取组织类型详情")
    @PreAuthorize("hasAuthority('system:org:view')")
    public Result<OrgType> getOrgTypeById(@PathVariable Long id) {
        return Result.success(orgTypeService.getOrgTypeById(id));
    }

    @GetMapping("/code/{typeCode}")
    @Operation(summary = "根据编码获取组织类型")
    @PreAuthorize("hasAuthority('system:org:view')")
    public Result<OrgType> getOrgTypeByCode(@PathVariable String typeCode) {
        return Result.success(orgTypeService.getOrgTypeByCode(typeCode));
    }

    @PostMapping
    @Operation(summary = "创建组织类型")
    @PreAuthorize("hasAuthority('system:org:create')")
    public Result<OrgType> createOrgType(@RequestBody CreateOrgTypeRequest request) {
        CreateOrgTypeCommand command = new CreateOrgTypeCommand();
        command.setTypeCode(request.getTypeCode());
        command.setTypeName(request.getTypeName());
        command.setParentTypeCode(request.getParentTypeCode());
        command.setLevelOrder(request.getLevelOrder());
        command.setIcon(request.getIcon());
        command.setColor(request.getColor());
        command.setDescription(request.getDescription());
        command.setCanHaveClasses(request.isCanHaveClasses());
        command.setCanHaveStudents(request.isCanHaveStudents());
        command.setCanBeInspected(request.isCanBeInspected());
        command.setCanHaveLeader(request.isCanHaveLeader());
        command.setSortOrder(request.getSortOrder());

        return Result.success(orgTypeService.createOrgType(command));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新组织类型")
    @PreAuthorize("hasAuthority('system:org:update')")
    public Result<OrgType> updateOrgType(@PathVariable Long id, @RequestBody UpdateOrgTypeRequest request) {
        UpdateOrgTypeCommand command = new UpdateOrgTypeCommand();
        command.setTypeName(request.getTypeName());
        command.setIcon(request.getIcon());
        command.setColor(request.getColor());
        command.setDescription(request.getDescription());
        command.setCanHaveClasses(request.isCanHaveClasses());
        command.setCanHaveStudents(request.isCanHaveStudents());
        command.setCanBeInspected(request.isCanBeInspected());
        command.setCanHaveLeader(request.isCanHaveLeader());
        command.setSortOrder(request.getSortOrder());

        return Result.success(orgTypeService.updateOrgType(id, command));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除组织类型")
    @PreAuthorize("hasAuthority('system:org:delete')")
    public Result<Void> deleteOrgType(@PathVariable Long id) {
        orgTypeService.deleteOrgType(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用组织类型")
    @PreAuthorize("hasAuthority('system:org:update')")
    public Result<OrgType> enableOrgType(@PathVariable Long id) {
        return Result.success(orgTypeService.toggleStatus(id, true));
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用组织类型")
    @PreAuthorize("hasAuthority('system:org:update')")
    public Result<OrgType> disableOrgType(@PathVariable Long id) {
        return Result.success(orgTypeService.toggleStatus(id, false));
    }

    // ==================== 请求对象 ====================

    public static class CreateOrgTypeRequest {
        private String typeCode;
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder = 0;
        private String icon;
        private String color;
        private String description;
        private boolean canHaveClasses;
        private boolean canHaveStudents;
        private boolean canBeInspected = true;
        private boolean canHaveLeader = true;
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
        public boolean isCanHaveClasses() { return canHaveClasses; }
        public void setCanHaveClasses(boolean canHaveClasses) { this.canHaveClasses = canHaveClasses; }
        public boolean isCanHaveStudents() { return canHaveStudents; }
        public void setCanHaveStudents(boolean canHaveStudents) { this.canHaveStudents = canHaveStudents; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public void setCanBeInspected(boolean canBeInspected) { this.canBeInspected = canBeInspected; }
        public boolean isCanHaveLeader() { return canHaveLeader; }
        public void setCanHaveLeader(boolean canHaveLeader) { this.canHaveLeader = canHaveLeader; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UpdateOrgTypeRequest {
        private String typeName;
        private String icon;
        private String color;
        private String description;
        private boolean canHaveClasses;
        private boolean canHaveStudents;
        private boolean canBeInspected;
        private boolean canHaveLeader;
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
        public boolean isCanHaveClasses() { return canHaveClasses; }
        public void setCanHaveClasses(boolean canHaveClasses) { this.canHaveClasses = canHaveClasses; }
        public boolean isCanHaveStudents() { return canHaveStudents; }
        public void setCanHaveStudents(boolean canHaveStudents) { this.canHaveStudents = canHaveStudents; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public void setCanBeInspected(boolean canBeInspected) { this.canBeInspected = canBeInspected; }
        public boolean isCanHaveLeader() { return canHaveLeader; }
        public void setCanHaveLeader(boolean canHaveLeader) { this.canHaveLeader = canHaveLeader; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}
