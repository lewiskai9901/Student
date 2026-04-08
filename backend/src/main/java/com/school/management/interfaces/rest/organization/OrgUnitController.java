package com.school.management.interfaces.rest.organization;

import com.school.management.application.organization.OrgUnitApplicationService;
import com.school.management.application.organization.command.CreateOrgUnitCommand;
import com.school.management.application.organization.command.UpdateOrgUnitCommand;
import com.school.management.application.organization.query.OrgUnitDTO;
import com.school.management.application.organization.query.OrgUnitTreeDTO;
import com.school.management.common.result.Result;
import com.school.management.domain.organization.model.entity.OrgType;
import com.school.management.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for organization unit management.
 */
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Organization Units", description = "Organization unit management API")
@RestController("orgUnitController")
@RequestMapping("/org-units")
public class OrgUnitController {

    private final OrgUnitApplicationService orgUnitService;

    @Operation(summary = "Create organization unit")
    @PostMapping
    @CasbinAccess(resource = "system:org", action = "create")
    public Result<OrgUnitDTO> createOrgUnit(@RequestBody CreateOrgUnitRequest request) {
        List<CreateOrgUnitCommand.SelectedPosition> selectedPositions = null;
        if (request.getSelectedPositions() != null) {
            selectedPositions = request.getSelectedPositions().stream().map(sp -> {
                CreateOrgUnitCommand.SelectedPosition pos = new CreateOrgUnitCommand.SelectedPosition();
                pos.setPositionName(sp.getPositionName());
                pos.setHeadcount(sp.getHeadcount());
                return pos;
            }).collect(Collectors.toList());
        }

        CreateOrgUnitCommand command = CreateOrgUnitCommand.builder()
            .unitCode(request.getUnitCode())
            .unitName(request.getUnitName())
            .unitType(request.getUnitType())
            .parentId(request.getParentId())
            .createdBy(SecurityUtils.requireCurrentUserId())
            .selectedPositions(selectedPositions)
            .attributes(request.getAttributes())
            .build();

        OrgUnitDTO result = orgUnitService.createOrgUnit(command);
        return Result.success(result);
    }

    @Operation(summary = "Update organization unit")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<OrgUnitDTO> updateOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id,
            @RequestBody UpdateOrgUnitRequest request) {
        UpdateOrgUnitCommand command = UpdateOrgUnitCommand.builder()
            .unitName(request.getUnitName())
            .sortOrder(request.getSortOrder())
            .headcount(request.getHeadcount())
            .attributes(request.getAttributes())
            .reason(request.getReason())
            .updatedBy(SecurityUtils.requireCurrentUserId())
            .build();

        OrgUnitDTO result = orgUnitService.updateOrgUnit(id, command);
        return Result.success(result);
    }

    @Operation(summary = "Get organization unit")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<OrgUnitDTO> getOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        OrgUnitDTO result = orgUnitService.getOrgUnit(id);
        return Result.success(result);
    }

    @Operation(summary = "Get organization tree")
    @GetMapping("/tree")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgUnitTreeDTO>> getOrgUnitTree() {
        List<OrgUnitTreeDTO> result = orgUnitService.getOrgUnitTree();
        return Result.success(result);
    }

    @Operation(summary = "Get by type")
    @GetMapping("/by-type/{type}")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgUnitDTO>> getByType(
            @Parameter(description = "Organization type code") @PathVariable String type) {
        List<OrgUnitDTO> result = orgUnitService.getOrgUnitsByType(type);
        return Result.success(result);
    }

    @Operation(summary = "Get allowed child types")
    @GetMapping("/allowed-child-types/{parentTypeCode}")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgType>> getAllowedChildTypes(
            @Parameter(description = "Parent type code") @PathVariable String parentTypeCode) {
        List<OrgType> result = orgUnitService.getAllowedChildTypes(parentTypeCode);
        return Result.success(result);
    }

    @Operation(summary = "Get all organization units")
    @GetMapping
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgUnitDTO>> getAll() {
        // Return flat list from tree
        List<OrgUnitTreeDTO> tree = orgUnitService.getOrgUnitTree();
        List<OrgUnitDTO> list = tree.stream()
            .map(t -> {
                OrgUnitDTO dto = new OrgUnitDTO();
                dto.setId(t.getId());
                dto.setUnitCode(t.getUnitCode());
                dto.setUnitName(t.getUnitName());
                dto.setUnitType(t.getUnitType());
                dto.setStatus(t.getStatus());
                dto.setStatusLabel(t.getStatusLabel());
                return dto;
            })
            .collect(java.util.stream.Collectors.toList());
        return Result.success(list);
    }

    @Operation(summary = "Get children")
    @GetMapping("/{id}/children")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgUnitDTO>> getChildren(
            @Parameter(description = "Parent ID") @PathVariable Long id) {
        List<OrgUnitDTO> result = orgUnitService.getChildren(id);
        return Result.success(result);
    }

    @Operation(summary = "Delete organization unit")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "system:org", action = "delete")
    public Result<Void> deleteOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        orgUnitService.deleteOrgUnit(id);
        return Result.success();
    }

    @Operation(summary = "Freeze organization unit")
    @PutMapping("/{id}/freeze")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<OrgUnitDTO> freezeOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id,
            @RequestBody(required = false) ReasonRequest request) {
        String reason = request != null ? request.getReason() : null;
        OrgUnitDTO result = orgUnitService.freezeOrgUnit(id, reason, SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Unfreeze organization unit")
    @PutMapping("/{id}/unfreeze")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<OrgUnitDTO> unfreezeOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        OrgUnitDTO result = orgUnitService.unfreezeOrgUnit(id, SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Dissolve organization unit")
    @PutMapping("/{id}/dissolve")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<OrgUnitDTO> dissolveOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id,
            @RequestBody ReasonRequest request) {
        OrgUnitDTO result = orgUnitService.dissolveOrgUnit(id, request.getReason(), SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Merge org unit into another")
    @PostMapping("/{sourceId}/merge-into/{targetId}")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<OrgUnitDTO> mergeOrgUnit(
            @Parameter(description = "Source org unit ID") @PathVariable Long sourceId,
            @Parameter(description = "Target org unit ID") @PathVariable Long targetId,
            @RequestBody(required = false) ReasonRequest request) {
        String reason = request != null ? request.getReason() : null;
        OrgUnitDTO result = orgUnitService.mergeOrgUnit(sourceId, targetId, reason, SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Split org unit into new units")
    @PostMapping("/{id}/split")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<List<OrgUnitDTO>> splitOrgUnit(
            @Parameter(description = "Source org unit ID") @PathVariable Long id,
            @RequestBody SplitOrgRequest request) {
        List<OrgUnitApplicationService.SplitRequest> splits = request.getSplits();
        List<OrgUnitDTO> result = orgUnitService.splitOrgUnit(id, splits, request.getReason(), SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Data
    public static class ReasonRequest {
        private String reason;
    }

    @Data
    public static class SplitOrgRequest {
        private String reason;
        private List<OrgUnitApplicationService.SplitRequest> splits;
    }

}
