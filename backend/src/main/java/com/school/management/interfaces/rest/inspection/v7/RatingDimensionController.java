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
 *
 * 路由设计:
 *   项目作用域:   /v7/insp/projects/{projectId}/rating-dimensions
 *   维度作用域:   /v7/insp/rating-dimensions/{id}
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class RatingDimensionController {

    private final RatingDimensionApplicationService dimensionService;

    // ===================== 项目作用域端点 =====================

    /**
     * GET /v7/insp/projects/{projectId}/rating-dimensions
     * 查询项目下所有评级维度
     */
    @GetMapping("/v7/insp/projects/{projectId}/rating-dimensions")
    @CasbinAccess(resource = "insp:rating", action = "view")
    public Result<List<RatingDimension>> listDimensionsByProject(@PathVariable Long projectId) {
        return Result.success(dimensionService.listDimensions(projectId));
    }

    /**
     * POST /v7/insp/projects/{projectId}/rating-dimensions
     * 在指定项目下创建评级维度
     */
    @PostMapping("/v7/insp/projects/{projectId}/rating-dimensions")
    @CasbinAccess(resource = "insp:rating", action = "create")
    public Result<RatingDimension> createDimensionUnderProject(@PathVariable Long projectId,
                                                                @RequestBody CreateDimensionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(dimensionService.createDimension(
                projectId, request.dimensionName(), request.sectionIds(),
                request.aggregation(), request.gradeBands(), request.awardName(),
                request.rankingEnabled(), userId));
    }

    // ===================== 维度作用域端点 =====================

    /**
     * POST /v7/insp/rating-dimensions
     * 创建评级维度（需在 body 中提供 projectId）
     */
    @PostMapping("/v7/insp/rating-dimensions")
    @CasbinAccess(resource = "insp:rating", action = "create")
    public Result<RatingDimension> createDimension(@RequestBody CreateDimensionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(dimensionService.createDimension(
                request.projectId(), request.dimensionName(), request.sectionIds(),
                request.aggregation(), request.gradeBands(), request.awardName(),
                request.rankingEnabled(), userId));
    }

    /**
     * GET /v7/insp/rating-dimensions
     * 查询项目下所有评级维度（通过 projectId 请求参数）
     */
    @GetMapping("/v7/insp/rating-dimensions")
    @CasbinAccess(resource = "insp:rating", action = "view")
    public Result<List<RatingDimension>> listDimensions(@RequestParam Long projectId) {
        return Result.success(dimensionService.listDimensions(projectId));
    }

    /**
     * GET /v7/insp/rating-dimensions/{id}
     * 获取单个评级维度详情
     */
    @GetMapping("/v7/insp/rating-dimensions/{id}")
    @CasbinAccess(resource = "insp:rating", action = "view")
    public Result<RatingDimension> getDimension(@PathVariable Long id) {
        return Result.success(dimensionService.getDimension(id)
                .orElseThrow(() -> new IllegalArgumentException("评级维度不存在: " + id)));
    }

    /**
     * PUT /v7/insp/rating-dimensions/{id}
     * 更新评级维度
     */
    @PutMapping("/v7/insp/rating-dimensions/{id}")
    @CasbinAccess(resource = "insp:rating", action = "edit")
    public Result<RatingDimension> updateDimension(@PathVariable Long id,
                                                    @RequestBody UpdateDimensionRequest request) {
        return Result.success(dimensionService.updateDimension(id,
                request.dimensionName(), request.sectionIds(),
                request.aggregation(), request.gradeBands(), request.awardName(),
                request.rankingEnabled()));
    }

    /**
     * DELETE /v7/insp/rating-dimensions/{id}
     * 删除评级维度
     */
    @DeleteMapping("/v7/insp/rating-dimensions/{id}")
    @CasbinAccess(resource = "insp:rating", action = "delete")
    public Result<Void> deleteDimension(@PathVariable Long id) {
        dimensionService.deleteDimension(id);
        return Result.success();
    }

    /**
     * POST /v7/insp/rating-dimensions/{id}/calculate
     * 跨周期评级计算（从 InspSubmission 聚合）
     *
     * 参数:
     *   cycleStart - 周期开始日期，格式 yyyy-MM-dd（必填）
     *   cycleEnd   - 周期结束日期，格式 yyyy-MM-dd（必填）
     *   cycleDate  - 向下兼容：单日计算（cycleStart=cycleEnd=cycleDate）
     */
    @PostMapping("/v7/insp/rating-dimensions/{id}/calculate")
    @CasbinAccess(resource = "insp:rating", action = "execute")
    public Result<List<RatingResult>> calculateRatings(
            @PathVariable Long id,
            @RequestParam(required = false) String cycleStart,
            @RequestParam(required = false) String cycleEnd,
            @RequestParam(required = false) String cycleDate) {

        if (cycleStart != null && cycleEnd != null) {
            // 跨周期计算
            return Result.success(dimensionService.calculateRatingFromSubmissions(
                    id, LocalDate.parse(cycleStart), LocalDate.parse(cycleEnd)));
        } else if (cycleDate != null) {
            // 向下兼容：单日计算
            LocalDate date = LocalDate.parse(cycleDate);
            return Result.success(dimensionService.calculateRatingFromSubmissions(id, date, date));
        } else {
            throw new IllegalArgumentException("必须提供 cycleStart+cycleEnd 或 cycleDate 参数");
        }
    }

    /**
     * GET /v7/insp/rating-dimensions/{id}/results
     * 查询指定周期的评级排名结果
     *
     * 参数:
     *   cycleDate - 周期标识日期（即计算时的 cycleStart），格式 yyyy-MM-dd
     */
    @GetMapping("/v7/insp/rating-dimensions/{id}/results")
    @CasbinAccess(resource = "insp:rating", action = "view")
    public Result<List<RatingResult>> getRatingResults(@PathVariable Long id,
                                                        @RequestParam String cycleDate) {
        return Result.success(dimensionService.getRankings(id, LocalDate.parse(cycleDate)));
    }

    /**
     * GET /v7/insp/rating-dimensions/{id}/rankings
     * 向下兼容的排名查询端点（与 /results 等效）
     */
    @GetMapping("/v7/insp/rating-dimensions/{id}/rankings")
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
