package com.school.management.interfaces.rest.organization;

import com.school.management.application.organization.OrgUnitApplicationService;
import com.school.management.application.organization.command.CreateOrgUnitCommand;
import com.school.management.application.organization.command.UpdateOrgUnitCommand;
import com.school.management.application.organization.query.OrgUnitDTO;
import com.school.management.application.organization.query.OrgUnitTreeDTO;
import com.school.management.common.result.Result;
import com.school.management.domain.organization.model.OrgUnitType;
import com.school.management.domain.organization.model.UnitCategory;
import com.school.management.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for organization unit management.
 * Provides endpoints for CRUD operations and tree queries.
 */
@Slf4j
@Tag(name = "Organization Units", description = "Organization unit management API")
@RestController("orgUnitControllerV2")
@RequestMapping("/v2/org-units")
public class OrgUnitController {

    private final OrgUnitApplicationService orgUnitService;

    public OrgUnitController(OrgUnitApplicationService orgUnitService) {
        this.orgUnitService = orgUnitService;
    }

    @Operation(summary = "Create organization unit", description = "Creates a new organization unit")
    @PostMapping
    @PreAuthorize("hasAuthority('system:org:create')")
    public Result<OrgUnitDTO> createOrgUnit(@RequestBody CreateOrgUnitRequest request) {
        CreateOrgUnitCommand command = CreateOrgUnitCommand.builder()
            .unitCode(request.getUnitCode())
            .unitName(request.getUnitName())
            .unitType(request.getUnitType())
            .unitCategory(request.getUnitCategory())
            .parentId(request.getParentId())
            .createdBy(getCurrentUserId())
            .build();

        OrgUnitDTO result = orgUnitService.createOrgUnit(command);
        return Result.success(result);
    }

    @Operation(summary = "Update organization unit", description = "Updates an existing organization unit")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:org:update')")
    public Result<OrgUnitDTO> updateOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id,
            @RequestBody UpdateOrgUnitRequest request) {
        UpdateOrgUnitCommand command = UpdateOrgUnitCommand.builder()
            .unitName(request.getUnitName())
            .unitCategory(request.getUnitCategory())
            .leaderId(request.getLeaderId())
            .deputyLeaderIds(request.getDeputyLeaderIds())
            .sortOrder(request.getSortOrder())
            .updatedBy(getCurrentUserId())
            .build();

        OrgUnitDTO result = orgUnitService.updateOrgUnit(id, command);
        return Result.success(result);
    }

    @Operation(summary = "Get organization unit", description = "Gets an organization unit by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:department:view')")
    public Result<OrgUnitDTO> getOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        OrgUnitDTO result = orgUnitService.getOrgUnit(id);
        return Result.success(result);
    }

    @Operation(summary = "Get organization tree", description = "Gets the complete organization tree")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:department:view') or hasAuthority('system:role:edit') or hasAuthority('system:role:view')")
    public Result<List<OrgUnitTreeDTO>> getOrgUnitTree() {
        List<OrgUnitTreeDTO> result = orgUnitService.getOrgUnitTree();
        return Result.success(result);
    }

    @Operation(summary = "Get by type", description = "Gets organization units by type")
    @GetMapping("/by-type/{type}")
    @PreAuthorize("hasAuthority('system:department:view')")
    public Result<List<OrgUnitDTO>> getByType(
            @Parameter(description = "Organization type") @PathVariable OrgUnitType type) {
        List<OrgUnitDTO> result = orgUnitService.getOrgUnitsByType(type);
        return Result.success(result);
    }

    @Operation(summary = "Get by category", description = "Gets organization units by category (functional, academic, administrative)")
    @GetMapping
    @PreAuthorize("hasAuthority('system:department:view') or hasAuthority('system:org:view')")
    public Result<List<OrgUnitDTO>> getByCategory(
            @Parameter(description = "Organization category") @RequestParam(required = false) String unitCategory) {
        if (unitCategory != null && !unitCategory.isEmpty()) {
            UnitCategory category = UnitCategory.fromCode(unitCategory);
            List<OrgUnitDTO> result = orgUnitService.getOrgUnitsByCategory(category);
            return Result.success(result);
        }
        // If no category specified, return all
        return Result.success(orgUnitService.getOrgUnitTree().stream()
            .map(tree -> {
                OrgUnitDTO dto = new OrgUnitDTO();
                dto.setId(tree.getId());
                dto.setUnitCode(tree.getUnitCode());
                dto.setUnitName(tree.getUnitName());
                dto.setUnitType(tree.getUnitType());
                dto.setEnabled(tree.getEnabled());
                return dto;
            })
            .collect(java.util.stream.Collectors.toList()));
    }

    @Operation(summary = "Get children", description = "Gets children of an organization unit")
    @GetMapping("/{id}/children")
    @PreAuthorize("hasAuthority('system:department:view')")
    public Result<List<OrgUnitDTO>> getChildren(
            @Parameter(description = "Parent ID") @PathVariable Long id) {
        List<OrgUnitDTO> result = orgUnitService.getChildren(id);
        return Result.success(result);
    }

    @Operation(summary = "Delete organization unit", description = "Deletes an organization unit")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:org:delete')")
    public Result<Void> deleteOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        orgUnitService.deleteOrgUnit(id);
        return Result.success();
    }

    @Operation(summary = "Enable organization unit", description = "Enables an organization unit")
    @PutMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('system:org:update')")
    public Result<Void> enableOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        orgUnitService.enableOrgUnit(id);
        return Result.success();
    }

    @Operation(summary = "Disable organization unit", description = "Disables an organization unit")
    @PutMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('system:org:update')")
    public Result<Void> disableOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        orgUnitService.disableOrgUnit(id);
        return Result.success();
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            log.warn("无法从安全上下文获取用户ID，使用默认值: {}", e.getMessage());
            return 1L;
        }
    }
}
