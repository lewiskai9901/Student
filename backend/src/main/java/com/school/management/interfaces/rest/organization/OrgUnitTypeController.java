package com.school.management.interfaces.rest.organization;

import com.school.management.application.organization.OrgUnitTypeApplicationService;
import com.school.management.application.organization.OrgUnitTypeApplicationService.*;
import com.school.management.application.shared.TypeTreeBuilder.TypeTreeNode;
import com.school.management.common.result.Result;
import com.school.management.domain.organization.model.entity.OrgType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import com.school.management.domain.organization.model.valueobject.PositionTemplate;

import java.util.List;
import java.util.Map;

/**
 * 组织类型管理控制器
 * API 路径: /org-types (统一类型系统规范)
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/org-types")
@Tag(name = "组织类型管理", description = "组织类型配置 API")
public class OrgUnitTypeController {

    private final OrgUnitTypeApplicationService orgUnitTypeService;

    // ==================== Category 枚举 ====================

    @GetMapping("/categories")
    @Operation(summary = "获取所有内置分类")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgCategoryDTO>> getCategories() {
        return Result.success(orgUnitTypeService.getCategories());
    }

    // ==================== 查询 ====================

    @GetMapping
    @Operation(summary = "获取所有组织类型")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgType>> getAllOrgTypes() {
        return Result.success(orgUnitTypeService.getAllOrgUnitTypes());
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的组织类型")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgType>> getEnabledOrgTypes() {
        return Result.success(orgUnitTypeService.getEnabledOrgUnitTypes());
    }

    @GetMapping("/tree")
    @Operation(summary = "获取组织类型树")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<TypeTreeNode<OrgType>>> getOrgTypeTree() {
        return Result.success(orgUnitTypeService.getOrgUnitTypeTree());
    }

    @GetMapping("/inspectable")
    @Operation(summary = "获取可检查的类型")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgType>> getInspectableTypes() {
        return Result.success(orgUnitTypeService.getInspectableTypes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取组织类型详情")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<OrgType> getOrgTypeById(@PathVariable Long id) {
        return Result.success(orgUnitTypeService.getOrgUnitTypeById(id));
    }

    @GetMapping("/code/{typeCode}")
    @Operation(summary = "根据编码获取组织类型")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<OrgType> getOrgTypeByCode(@PathVariable String typeCode) {
        return Result.success(orgUnitTypeService.getOrgUnitTypeByCode(typeCode));
    }

    // ==================== 写操作 ====================

    @PostMapping
    @Operation(summary = "创建组织类型")
    @CasbinAccess(resource = "system:org", action = "create")
    public Result<OrgType> createOrgType(@RequestBody CreateOrgTypeRequest request) {
        CreateOrgUnitTypeCommand command = new CreateOrgUnitTypeCommand();
        command.setTypeCode(request.getTypeCode());
        command.setTypeName(request.getTypeName());
        command.setCategory(request.getCategory());
        command.setParentTypeCode(request.getParentTypeCode());
        command.setIcon(request.getIcon());
        command.setDescription(request.getDescription());
        command.setFeatures(request.getFeatures());
        command.setMetadataSchema(request.getMetadataSchema());
        command.setAllowedChildTypeCodes(request.getAllowedChildTypeCodes());
        command.setMaxDepth(request.getMaxDepth());
        command.setDefaultUserTypeCodes(request.getDefaultUserTypeCodes());
        command.setDefaultPlaceTypeCodes(request.getDefaultPlaceTypeCodes());
        command.setDefaultPositions(request.getDefaultPositions());
        command.setSortOrder(request.getSortOrder());

        return Result.success(orgUnitTypeService.createOrgUnitType(command));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新组织类型")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<OrgType> updateOrgType(@PathVariable Long id, @RequestBody UpdateOrgTypeRequest request) {
        UpdateOrgUnitTypeCommand command = new UpdateOrgUnitTypeCommand();
        command.setTypeName(request.getTypeName());
        command.setCategory(request.getCategory());
        command.setIcon(request.getIcon());
        command.setDescription(request.getDescription());
        command.setFeatures(request.getFeatures());
        command.setMetadataSchema(request.getMetadataSchema());
        command.setAllowedChildTypeCodes(request.getAllowedChildTypeCodes());
        command.setMaxDepth(request.getMaxDepth());
        command.setDefaultUserTypeCodes(request.getDefaultUserTypeCodes());
        command.setDefaultPlaceTypeCodes(request.getDefaultPlaceTypeCodes());
        command.setDefaultPositions(request.getDefaultPositions());
        command.setSortOrder(request.getSortOrder());

        return Result.success(orgUnitTypeService.updateOrgUnitType(id, command));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除组织类型")
    @CasbinAccess(resource = "system:org", action = "delete")
    public Result<Void> deleteOrgType(@PathVariable Long id) {
        orgUnitTypeService.deleteOrgUnitType(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用组织类型")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<OrgType> enableOrgType(@PathVariable Long id) {
        return Result.success(orgUnitTypeService.toggleStatus(id, true));
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用组织类型")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<OrgType> disableOrgType(@PathVariable Long id) {
        return Result.success(orgUnitTypeService.toggleStatus(id, false));
    }

    // ==================== 请求对象 ====================

    public static class CreateOrgTypeRequest {
        private String typeCode;
        private String typeName;
        private String category;
        private String parentTypeCode;
        private String icon;
        private String description;
        private Map<String, Boolean> features;
        private String metadataSchema;
        private List<String> allowedChildTypeCodes;
        private Integer maxDepth;
        private List<String> defaultUserTypeCodes;
        private List<String> defaultPlaceTypeCodes;
        private List<PositionTemplate> defaultPositions;
        private Integer sortOrder = 0;

        public String getTypeCode() { return typeCode; }
        public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getParentTypeCode() { return parentTypeCode; }
        public void setParentTypeCode(String parentTypeCode) { this.parentTypeCode = parentTypeCode; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Boolean> getFeatures() { return features; }
        public void setFeatures(Map<String, Boolean> features) { this.features = features; }
        public String getMetadataSchema() { return metadataSchema; }
        public void setMetadataSchema(String metadataSchema) { this.metadataSchema = metadataSchema; }
        public List<String> getAllowedChildTypeCodes() { return allowedChildTypeCodes; }
        public void setAllowedChildTypeCodes(List<String> allowedChildTypeCodes) { this.allowedChildTypeCodes = allowedChildTypeCodes; }
        public Integer getMaxDepth() { return maxDepth; }
        public void setMaxDepth(Integer maxDepth) { this.maxDepth = maxDepth; }
        public List<String> getDefaultUserTypeCodes() { return defaultUserTypeCodes; }
        public void setDefaultUserTypeCodes(List<String> defaultUserTypeCodes) { this.defaultUserTypeCodes = defaultUserTypeCodes; }
        public List<String> getDefaultPlaceTypeCodes() { return defaultPlaceTypeCodes; }
        public void setDefaultPlaceTypeCodes(List<String> defaultPlaceTypeCodes) { this.defaultPlaceTypeCodes = defaultPlaceTypeCodes; }
        public List<PositionTemplate> getDefaultPositions() { return defaultPositions; }
        public void setDefaultPositions(List<PositionTemplate> defaultPositions) { this.defaultPositions = defaultPositions; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UpdateOrgTypeRequest {
        private String typeName;
        private String category;
        private String icon;
        private String description;
        private Map<String, Boolean> features;
        private String metadataSchema;
        private List<String> allowedChildTypeCodes;
        private Integer maxDepth;
        private List<String> defaultUserTypeCodes;
        private List<String> defaultPlaceTypeCodes;
        private List<PositionTemplate> defaultPositions;
        private Integer sortOrder;

        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Boolean> getFeatures() { return features; }
        public void setFeatures(Map<String, Boolean> features) { this.features = features; }
        public String getMetadataSchema() { return metadataSchema; }
        public void setMetadataSchema(String metadataSchema) { this.metadataSchema = metadataSchema; }
        public List<String> getAllowedChildTypeCodes() { return allowedChildTypeCodes; }
        public void setAllowedChildTypeCodes(List<String> allowedChildTypeCodes) { this.allowedChildTypeCodes = allowedChildTypeCodes; }
        public Integer getMaxDepth() { return maxDepth; }
        public void setMaxDepth(Integer maxDepth) { this.maxDepth = maxDepth; }
        public List<String> getDefaultUserTypeCodes() { return defaultUserTypeCodes; }
        public void setDefaultUserTypeCodes(List<String> defaultUserTypeCodes) { this.defaultUserTypeCodes = defaultUserTypeCodes; }
        public List<String> getDefaultPlaceTypeCodes() { return defaultPlaceTypeCodes; }
        public void setDefaultPlaceTypeCodes(List<String> defaultPlaceTypeCodes) { this.defaultPlaceTypeCodes = defaultPlaceTypeCodes; }
        public List<PositionTemplate> getDefaultPositions() { return defaultPositions; }
        public void setDefaultPositions(List<PositionTemplate> defaultPositions) { this.defaultPositions = defaultPositions; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}
