package com.school.management.interfaces.rest.organization;

import com.school.management.application.organization.PositionApplicationService;
import com.school.management.application.organization.command.CreatePositionCommand;
import com.school.management.application.organization.command.UpdatePositionCommand;
import com.school.management.application.organization.query.PositionDTO;
import com.school.management.application.organization.query.PositionStaffingDTO;
import com.school.management.application.organization.query.UserPositionDTO;
import com.school.management.application.organization.UserPositionApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Positions", description = "岗位管理")
@RestController
@RequestMapping("/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionApplicationService positionService;
    private final UserPositionApplicationService userPositionService;

    @Operation(summary = "按组织查岗位列表")
    @GetMapping
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<PositionDTO>> list(@RequestParam Long orgUnitId) {
        return Result.success(positionService.getPositionsByOrgUnit(orgUnitId));
    }

    @Operation(summary = "岗位详情")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<PositionDTO> get(@PathVariable Long id) {
        return Result.success(positionService.getPosition(id));
    }

    @Operation(summary = "创建岗位")
    @PostMapping
    @CasbinAccess(resource = "system:org", action = "create")
    public Result<PositionDTO> create(@RequestBody CreatePositionRequest request) {
        CreatePositionCommand cmd = CreatePositionCommand.builder()
            .positionCode(request.getPositionCode())
            .positionName(request.getPositionName())
            .orgUnitId(request.getOrgUnitId())
            .headcount(request.getHeadcount())
            .jobLevel(request.getJobLevel())
            .reportsToId(request.getReportsToId())
            .responsibilities(request.getResponsibilities())
            .requirements(request.getRequirements())
            .keyPosition(request.isKeyPosition())
            .createdBy(SecurityUtils.requireCurrentUserId())
            .build();
        return Result.success(positionService.createPosition(cmd));
    }

    @Operation(summary = "更新岗位")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<PositionDTO> update(@PathVariable Long id, @RequestBody UpdatePositionRequest request) {
        UpdatePositionCommand cmd = UpdatePositionCommand.builder()
            .positionName(request.getPositionName())
            .jobLevel(request.getJobLevel())
            .headcount(request.getHeadcount())
            .reportsToId(request.getReportsToId())
            .responsibilities(request.getResponsibilities())
            .requirements(request.getRequirements())
            .keyPosition(request.isKeyPosition())
            .sortOrder(request.getSortOrder())
            .updatedBy(SecurityUtils.requireCurrentUserId())
            .build();
        return Result.success(positionService.updatePosition(id, cmd));
    }

    @Operation(summary = "删除岗位")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "system:org", action = "delete")
    public Result<Void> delete(@PathVariable Long id) {
        positionService.deletePosition(id);
        return Result.success();
    }

    @Operation(summary = "启用岗位")
    @PutMapping("/{id}/enable")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<Void> enable(@PathVariable Long id) {
        positionService.enablePosition(id);
        return Result.success();
    }

    @Operation(summary = "禁用岗位")
    @PutMapping("/{id}/disable")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<Void> disable(@PathVariable Long id) {
        positionService.disablePosition(id);
        return Result.success();
    }

    @Operation(summary = "岗位在编人员")
    @GetMapping("/{id}/holders")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<UserPositionDTO>> getHolders(@PathVariable Long id) {
        return Result.success(userPositionService.getCurrentByPosition(id));
    }

    @Operation(summary = "组织编制统计")
    @GetMapping("/staffing")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<PositionStaffingDTO> getStaffing(@RequestParam Long orgUnitId) {
        return Result.success(positionService.getStaffing(orgUnitId));
    }

    // ==================== Request DTOs ====================

    @Data
    public static class CreatePositionRequest {
        private String positionCode;
        private String positionName;
        private Long orgUnitId;
        private Integer headcount;
        private String jobLevel;
        private Long reportsToId;
        private String responsibilities;
        private String requirements;
        private boolean keyPosition;
    }

    @Data
    public static class UpdatePositionRequest {
        private String positionName;
        private String jobLevel;
        private Integer headcount;
        private Long reportsToId;
        private String responsibilities;
        private String requirements;
        private boolean keyPosition;
        private Integer sortOrder;
    }
}
