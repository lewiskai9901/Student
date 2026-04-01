package com.school.management.interfaces.rest.academic;

import com.school.management.application.academic.MajorApplicationService;
import com.school.management.application.academic.command.CreateMajorDirectionCommand;
import com.school.management.application.academic.command.UpdateMajorDirectionCommand;
import com.school.management.application.academic.query.MajorDirectionDTO;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 专业方向 REST 控制器
 */
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Major Directions", description = "Major direction management API")
@RestController
@RequestMapping("/academic/major-directions")
public class MajorDirectionController {

    private final MajorApplicationService majorService;

    @Operation(summary = "Get directions by major")
    @GetMapping("/major/{majorId}")
    @CasbinAccess(resource = "major", action = "list")
    public Result<List<MajorDirectionDTO>> getDirectionsByMajor(@PathVariable Long majorId) {
        return Result.success(majorService.getDirectionsByMajor(majorId));
    }

    @Operation(summary = "Get all directions")
    @GetMapping("/all")
    @CasbinAccess(resource = "major", action = "list")
    public Result<List<MajorDirectionDTO>> getAllDirections() {
        return Result.success(majorService.getAllDirections());
    }

    @Operation(summary = "Get direction detail")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "major", action = "list")
    public Result<MajorDirectionDTO> getDirection(@PathVariable Long id) {
        return Result.success(majorService.getDirection(id));
    }

    @Operation(summary = "Create direction")
    @PostMapping
    @CasbinAccess(resource = "major", action = "add")
    public Result<MajorDirectionDTO> createDirection(@Valid @RequestBody CreateMajorDirectionRequest request) {
        CreateMajorDirectionCommand command = new CreateMajorDirectionCommand();
        command.setMajorId(request.getMajorId());
        command.setDirectionCode(request.getDirectionCode());
        command.setDirectionName(request.getDirectionName());
        command.setLevel(request.getLevel());
        command.setYears(request.getYears());
        command.setIsSegmented(request.getIsSegmented());
        command.setPhase1Level(request.getPhase1Level());
        command.setPhase1Years(request.getPhase1Years());
        command.setPhase2Level(request.getPhase2Level());
        command.setPhase2Years(request.getPhase2Years());
        command.setRemarks(request.getRemarks());
        command.setCreatedBy(SecurityUtils.requireCurrentUserId());

        return Result.success(majorService.createDirection(command));
    }

    @Operation(summary = "Update direction")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "major", action = "edit")
    public Result<MajorDirectionDTO> updateDirection(@PathVariable Long id,
                                                      @RequestBody UpdateMajorDirectionRequest request) {
        UpdateMajorDirectionCommand command = new UpdateMajorDirectionCommand();
        command.setDirectionName(request.getDirectionName());
        command.setLevel(request.getLevel());
        command.setYears(request.getYears());
        command.setIsSegmented(request.getIsSegmented());
        command.setPhase1Level(request.getPhase1Level());
        command.setPhase1Years(request.getPhase1Years());
        command.setPhase2Level(request.getPhase2Level());
        command.setPhase2Years(request.getPhase2Years());
        command.setRemarks(request.getRemarks());
        command.setUpdatedBy(SecurityUtils.requireCurrentUserId());

        return Result.success(majorService.updateDirection(id, command));
    }

    @Operation(summary = "Delete direction")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "major", action = "delete")
    public Result<Void> deleteDirection(@PathVariable Long id) {
        majorService.deleteDirection(id);
        return Result.success();
    }

    @Operation(summary = "Batch delete directions")
    @DeleteMapping("/batch")
    @CasbinAccess(resource = "major", action = "delete")
    public Result<Void> batchDeleteDirections(@RequestBody List<Long> ids) {
        ids.forEach(majorService::deleteDirection);
        return Result.success();
    }

}
