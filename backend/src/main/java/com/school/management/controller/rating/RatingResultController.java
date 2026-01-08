package com.school.management.controller.rating;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.Result;
import com.school.management.dto.rating.RatingResultQueryDTO;
import com.school.management.dto.rating.RatingResultVO;
import com.school.management.security.CustomUserDetails;
import com.school.management.service.rating.RatingResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评级结果控制器
 *
 * @author System
 * @since 4.4.0
 */
@Tag(name = "评级结果管理")
@RestController
@RequestMapping("/quantification/rating/result")
@RequiredArgsConstructor
public class RatingResultController {

    private final RatingResultService ratingResultService;

    @Operation(summary = "审核评级结果")
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('quantification:check-record:review')")
    public Result<Void> approveResult(
            @PathVariable Long id,
            @RequestParam boolean approved,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ratingResultService.approveResult(id, approved, userDetails.getUserId());
        return Result.success();
    }

    @Operation(summary = "批量审核评级结果")
    @PutMapping("/batch-approve")
    @PreAuthorize("hasAuthority('quantification:check-record:review')")
    public Result<Void> batchApproveResults(
            @RequestBody List<Long> resultIds,
            @RequestParam boolean approved,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ratingResultService.batchApproveResults(resultIds, approved, userDetails.getUserId());
        return Result.success();
    }

    @Operation(summary = "发布评级结果")
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('quantification:check-record:publish')")
    public Result<Void> publishResult(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ratingResultService.publishResult(id, userDetails.getUserId());
        return Result.success();
    }

    @Operation(summary = "批量发布评级结果")
    @PutMapping("/batch-publish")
    @PreAuthorize("hasAuthority('quantification:check-record:publish')")
    public Result<Void> batchPublishResults(
            @RequestBody List<Long> resultIds,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ratingResultService.batchPublishResults(resultIds, userDetails.getUserId());
        return Result.success();
    }

    @Operation(summary = "撤销发布")
    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('quantification:check-record:publish')")
    public Result<Void> revokeResult(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ratingResultService.revokeResult(id, userDetails.getUserId());
        return Result.success();
    }

    @Operation(summary = "获取评级结果详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('quantification:config:view')")
    public Result<RatingResultVO> getResultDetail(@PathVariable Long id) {
        RatingResultVO result = ratingResultService.getResultDetail(id);
        return Result.success(result);
    }

    @Operation(summary = "分页查询评级结果")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('quantification:config:view')")
    public Result<Page<RatingResultVO>> getResultPage(@Validated RatingResultQueryDTO query) {
        Page<RatingResultVO> page = ratingResultService.getResultPage(query);
        return Result.success(page);
    }

    @Operation(summary = "获取班级的评级历史")
    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAuthority('quantification:config:view')")
    public Result<List<RatingResultVO>> getClassRatingHistory(
            @PathVariable Long classId,
            @RequestParam(required = false) Long ratingConfigId) {
        List<RatingResultVO> history = ratingResultService.getClassRatingHistory(classId, ratingConfigId);
        return Result.success(history);
    }
}
