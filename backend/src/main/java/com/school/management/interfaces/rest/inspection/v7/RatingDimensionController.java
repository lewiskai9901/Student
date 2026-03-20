package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.RatingDimensionApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.scoring.RatingDimension;
import com.school.management.domain.inspection.model.v7.scoring.RatingResult;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 评级维度控制器
 * 管理评级维度配置，计算评级结果和排名。
 */
@Slf4j
@RestController
@RequestMapping("/v7/insp/rating-dimensions")
@RequiredArgsConstructor
public class RatingDimensionController {

    private final RatingDimensionApplicationService dimensionService;

    @PostMapping
    @CasbinAccess(resource = "insp:rating", action = "create")
    public Result<RatingDimension> createDimension(@RequestBody CreateDimensionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(dimensionService.createDimension(
                request.projectId(), request.dimensionName(), request.sectionIds(),
                request.aggregation(), request.gradeBands(), request.awardName(),
                request.rankingEnabled(), userId));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:rating", action = "view")
    public Result<List<RatingDimension>> listDimensions(@RequestParam Long projectId) {
        return Result.success(dimensionService.listDimensions(projectId));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:rating", action = "view")
    public Result<RatingDimension> getDimension(@PathVariable Long id) {
        return Result.success(dimensionService.getDimension(id)
                .orElseThrow(() -> new IllegalArgumentException("评级维度不存在: " + id)));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:rating", action = "edit")
    public Result<RatingDimension> updateDimension(@PathVariable Long id,
                                                    @RequestBody UpdateDimensionRequest request) {
        return Result.success(dimensionService.updateDimension(id,
                request.dimensionName(), request.sectionIds(),
                request.aggregation(), request.gradeBands(), request.awardName(),
                request.rankingEnabled()));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:rating", action = "delete")
    public Result<Void> deleteDimension(@PathVariable Long id) {
        dimensionService.deleteDimension(id);
        return Result.success();
    }

    @PostMapping("/{id}/calculate")
    @CasbinAccess(resource = "insp:rating", action = "execute")
    public Result<List<RatingResult>> calculateRatings(@PathVariable Long id,
                                                        @RequestParam String cycleDate) {
        return Result.success(dimensionService.calculateRatings(id, LocalDate.parse(cycleDate)));
    }

    @GetMapping("/{id}/rankings")
    @CasbinAccess(resource = "insp:rating", action = "view")
    public Result<List<RatingResult>> getRankings(@PathVariable Long id,
                                                   @RequestParam String cycleDate) {
        return Result.success(dimensionService.getRankings(id, LocalDate.parse(cycleDate)));
    }

    // --- Request DTOs ---

    public record CreateDimensionRequest(
            Long projectId,
            String dimensionName,
            String sectionIds,
            String aggregation,
            String gradeBands,
            String awardName,
            Boolean rankingEnabled
    ) {}

    public record UpdateDimensionRequest(
            String dimensionName,
            String sectionIds,
            String aggregation,
            String gradeBands,
            String awardName,
            Boolean rankingEnabled
    ) {}
}
