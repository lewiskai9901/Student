package com.school.management.interfaces.rest.academic;

import com.school.management.application.academic.GradeMajorDirectionApplicationService;
import com.school.management.application.academic.query.GradeMajorDirectionDTO;
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
 * 年级-专业方向关联 REST 控制器
 */
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Grade Major Directions", description = "Grade-major-direction association API")
@RestController
@RequestMapping("/academic/grade-major-directions")
public class GradeMajorDirectionController {

    private final GradeMajorDirectionApplicationService service;

    @Operation(summary = "Get directions by enrollment year")
    @GetMapping("/year/{year}")
    @CasbinAccess(resource = "academic:grade-direction", action = "view")
    public Result<List<GradeMajorDirectionDTO>> getDirectionsByYear(@PathVariable Integer year) {
        return Result.success(service.getDirectionsByYear(year));
    }

    @Operation(summary = "Get directions by major direction ID")
    @GetMapping("/direction/{directionId}")
    @CasbinAccess(resource = "academic:grade-direction", action = "view")
    public Result<List<GradeMajorDirectionDTO>> getDirectionsByMajorDirection(@PathVariable Long directionId) {
        return Result.success(service.getDirectionsByMajorDirectionId(directionId));
    }

    @Operation(summary = "Get by year and direction")
    @GetMapping("/year/{year}/direction/{directionId}")
    @CasbinAccess(resource = "academic:grade-direction", action = "view")
    public Result<GradeMajorDirectionDTO> getByYearAndDirection(@PathVariable Integer year,
                                                                  @PathVariable Long directionId) {
        return Result.success(service.getByYearAndDirection(year, directionId));
    }

    @Operation(summary = "Get by ID")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "academic:grade-direction", action = "view")
    public Result<GradeMajorDirectionDTO> getById(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @Operation(summary = "Add direction to year")
    @PostMapping
    @CasbinAccess(resource = "academic:grade-direction", action = "edit")
    public Result<GradeMajorDirectionDTO> addDirectionToYear(
            @Valid @RequestBody CreateGradeMajorDirectionRequest request) {
        return Result.success(service.addDirectionToYear(
                request.getAcademicYear(),
                request.getMajorDirectionId(),
                request.getRemarks(),
                SecurityUtils.requireCurrentUserId()
        ));
    }

    @Operation(summary = "Batch add directions to year")
    @PostMapping("/year/{year}/batch")
    @CasbinAccess(resource = "academic:grade-direction", action = "edit")
    public Result<Void> batchAddDirectionsToYear(@PathVariable Integer year,
                                                  @RequestBody List<Long> directionIds) {
        service.batchAddDirectionsToYear(year, directionIds, SecurityUtils.requireCurrentUserId());
        return Result.success();
    }

    @Operation(summary = "Update grade-major-direction")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "academic:grade-direction", action = "edit")
    public Result<GradeMajorDirectionDTO> updateGradeMajorDirection(
            @PathVariable Long id,
            @RequestBody UpdateGradeMajorDirectionRequest request) {
        return Result.success(service.updateGradeMajorDirection(id, request.getRemarks(), SecurityUtils.requireCurrentUserId()));
    }

    @Operation(summary = "Delete grade-major-direction")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "academic:grade-direction", action = "edit")
    public Result<Void> deleteGradeMajorDirection(@PathVariable Long id) {
        service.deleteGradeMajorDirection(id);
        return Result.success();
    }

    @Operation(summary = "Batch delete grade-major-directions")
    @DeleteMapping("/batch")
    @CasbinAccess(resource = "academic:grade-direction", action = "edit")
    public Result<Void> batchDeleteGradeMajorDirections(@RequestBody List<Long> ids) {
        service.batchDeleteGradeMajorDirections(ids);
        return Result.success();
    }

}
