package com.school.management.interfaces.rest.rating;

import com.school.management.application.rating.*;
import com.school.management.common.result.Result;
import com.school.management.domain.rating.model.*;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * V2 REST controller for rating management.
 */
@RestController
@RequestMapping("/v2/ratings")
@Tag(name = "Ratings V2", description = "Rating management API (V2)")
@RequiredArgsConstructor
public class RatingControllerV2 {

    private final RatingApplicationService ratingService;
    private final JwtTokenService jwtTokenService;

    // ========== Config Endpoints ==========

    @PostMapping("/configs")
    @Operation(summary = "Create a rating configuration")
    @PreAuthorize("hasAuthority('quantification:config:add')")
    public Result<RatingConfigResponse> createConfig(@Valid @RequestBody CreateRatingConfigRequest request) {
        CreateRatingConfigCommand command = CreateRatingConfigCommand.builder()
            .checkPlanId(request.getCheckPlanId())
            .ratingName(request.getRatingName())
            .periodType(RatingPeriodType.fromString(request.getPeriodType()))
            .divisionMethod(DivisionMethod.fromString(request.getDivisionMethod()))
            .divisionValue(request.getDivisionValue())
            .icon(request.getIcon())
            .color(request.getColor())
            .priority(request.getPriority())
            .description(request.getDescription())
            .requireApproval(request.isRequireApproval())
            .autoPublish(request.isAutoPublish())
            .createdBy(getCurrentUserId())
            .build();

        RatingConfig config = ratingService.createConfig(command);
        return Result.success(toConfigResponse(config));
    }

    @PutMapping("/configs/{id}")
    @Operation(summary = "Update a rating configuration")
    @PreAuthorize("hasAuthority('quantification:config:edit')")
    public Result<RatingConfigResponse> updateConfig(@PathVariable Long id,
            @Valid @RequestBody UpdateRatingConfigRequest request) {
        UpdateRatingConfigCommand command = UpdateRatingConfigCommand.builder()
            .ratingName(request.getRatingName())
            .periodType(request.getPeriodType() != null ? RatingPeriodType.fromString(request.getPeriodType()) : null)
            .divisionMethod(request.getDivisionMethod() != null ? DivisionMethod.fromString(request.getDivisionMethod()) : null)
            .divisionValue(request.getDivisionValue())
            .icon(request.getIcon())
            .color(request.getColor())
            .priority(request.getPriority())
            .description(request.getDescription())
            .requireApproval(request.getRequireApproval())
            .autoPublish(request.getAutoPublish())
            .build();

        RatingConfig config = ratingService.updateConfig(id, command);
        return Result.success(toConfigResponse(config));
    }

    @DeleteMapping("/configs/{id}")
    @Operation(summary = "Delete a rating configuration")
    @PreAuthorize("hasAuthority('quantification:config:delete')")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        ratingService.deleteConfig(id);
        return Result.success();
    }

    @PutMapping("/configs/{id}/toggle")
    @Operation(summary = "Toggle config enabled status")
    @PreAuthorize("hasAuthority('quantification:config:edit')")
    public Result<RatingConfigResponse> toggleConfigEnabled(@PathVariable Long id,
            @RequestParam boolean enabled) {
        RatingConfig config = ratingService.toggleConfigEnabled(id, enabled);
        return Result.success(toConfigResponse(config));
    }

    @GetMapping("/configs/{id}")
    @Operation(summary = "Get config by ID")
    @PreAuthorize("hasAuthority('quantification:config:view')")
    public Result<RatingConfigResponse> getConfig(@PathVariable Long id) {
        return ratingService.getConfig(id)
            .map(c -> Result.success(toConfigResponse(c)))
            .orElse(Result.error("Config not found"));
    }

    @GetMapping("/configs/plan/{checkPlanId}")
    @Operation(summary = "Get configs by check plan")
    @PreAuthorize("hasAuthority('quantification:config:view')")
    public Result<List<RatingConfigResponse>> getConfigsByPlan(@PathVariable Long checkPlanId) {
        List<RatingConfigResponse> configs = ratingService.getConfigsByCheckPlan(checkPlanId)
            .stream().map(this::toConfigResponse).collect(Collectors.toList());
        return Result.success(configs);
    }

    // ========== Result Endpoints ==========

    @GetMapping("/results/{id}")
    @Operation(summary = "Get result by ID")
    @PreAuthorize("hasAuthority('quantification:config:view')")
    public Result<RatingResultResponse> getResult(@PathVariable Long id) {
        return ratingService.getResult(id)
            .map(r -> Result.success(toResultResponse(r)))
            .orElse(Result.error("Result not found"));
    }

    @GetMapping("/results/pending")
    @Operation(summary = "Get results pending approval")
    @PreAuthorize("hasAuthority('quantification:check-record:review')")
    public Result<List<RatingResultResponse>> getPendingResults() {
        List<RatingResultResponse> results = ratingService.getPendingApprovalResults()
            .stream().map(this::toResultResponse).collect(Collectors.toList());
        return Result.success(results);
    }

    @GetMapping("/results/class/{classId}")
    @Operation(summary = "Get results by class")
    @PreAuthorize("hasAuthority('quantification:config:view')")
    public Result<List<RatingResultResponse>> getResultsByClass(@PathVariable Long classId) {
        List<RatingResultResponse> results = ratingService.getResultsByClass(classId)
            .stream().map(this::toResultResponse).collect(Collectors.toList());
        return Result.success(results);
    }

    @PutMapping("/results/{id}/approve")
    @Operation(summary = "Approve a rating result")
    @PreAuthorize("hasAuthority('quantification:check-record:review')")
    public Result<RatingResultResponse> approveResult(@PathVariable Long id,
            @RequestParam(required = false) String comment) {
        RatingResult result = ratingService.approveResult(id, getCurrentUserId(), comment);
        return Result.success(toResultResponse(result));
    }

    @PutMapping("/results/batch-approve")
    @Operation(summary = "Batch approve rating results")
    @PreAuthorize("hasAuthority('quantification:check-record:review')")
    public Result<List<RatingResultResponse>> batchApproveResults(@RequestBody List<Long> resultIds,
            @RequestParam(required = false) String comment) {
        List<RatingResultResponse> results = ratingService.batchApproveResults(resultIds, getCurrentUserId(), comment)
            .stream().map(this::toResultResponse).collect(Collectors.toList());
        return Result.success(results);
    }

    @PutMapping("/results/{id}/reject")
    @Operation(summary = "Reject a rating result")
    @PreAuthorize("hasAuthority('quantification:check-record:review')")
    public Result<RatingResultResponse> rejectResult(@PathVariable Long id,
            @RequestParam String reason) {
        RatingResult result = ratingService.rejectResult(id, getCurrentUserId(), reason);
        return Result.success(toResultResponse(result));
    }

    @PutMapping("/results/{id}/publish")
    @Operation(summary = "Publish a rating result")
    @PreAuthorize("hasAuthority('quantification:check-record:publish')")
    public Result<RatingResultResponse> publishResult(@PathVariable Long id) {
        RatingResult result = ratingService.publishResult(id, getCurrentUserId());
        return Result.success(toResultResponse(result));
    }

    @PutMapping("/results/batch-publish")
    @Operation(summary = "Batch publish rating results")
    @PreAuthorize("hasAuthority('quantification:check-record:publish')")
    public Result<List<RatingResultResponse>> batchPublishResults(@RequestBody List<Long> resultIds) {
        List<RatingResultResponse> results = ratingService.batchPublishResults(resultIds, getCurrentUserId())
            .stream().map(this::toResultResponse).collect(Collectors.toList());
        return Result.success(results);
    }

    @PutMapping("/results/{id}/revoke")
    @Operation(summary = "Revoke a published result")
    @PreAuthorize("hasAuthority('quantification:check-record:publish')")
    public Result<RatingResultResponse> revokeResult(@PathVariable Long id) {
        RatingResult result = ratingService.revokeResult(id, getCurrentUserId());
        return Result.success(toResultResponse(result));
    }

    // ========== Helper Methods ==========

    private Long getCurrentUserId() {
        return jwtTokenService.getCurrentUserId();
    }

    private RatingConfigResponse toConfigResponse(RatingConfig c) {
        RatingConfigResponse r = new RatingConfigResponse();
        r.setId(c.getId());
        r.setCheckPlanId(c.getCheckPlanId());
        r.setRatingName(c.getRatingName());
        r.setPeriodType(c.getPeriodType());
        r.setIcon(c.getIcon());
        r.setColor(c.getColor());
        r.setPriority(c.getPriority());
        r.setDivisionMethod(c.getDivisionMethod());
        r.setDivisionValue(c.getDivisionValue());
        r.setRequireApproval(c.isRequireApproval());
        r.setAutoPublish(c.isAutoPublish());
        r.setEnabled(c.isEnabled());
        r.setDescription(c.getDescription());
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
        return r;
    }

    private RatingResultResponse toResultResponse(RatingResult r) {
        RatingResultResponse resp = new RatingResultResponse();
        resp.setId(r.getId());
        resp.setRatingConfigId(r.getRatingConfigId());
        resp.setCheckPlanId(r.getCheckPlanId());
        resp.setClassId(r.getClassId());
        resp.setClassName(r.getClassName());
        resp.setPeriodType(r.getPeriodType());
        resp.setPeriodStart(r.getPeriodStart());
        resp.setPeriodEnd(r.getPeriodEnd());
        resp.setRanking(r.getRanking());
        resp.setFinalScore(r.getFinalScore());
        resp.setAwarded(r.isAwarded());
        resp.setStatus(r.getStatus());
        resp.setCalculatedAt(r.getCalculatedAt());
        resp.setApprovedBy(r.getApprovedBy());
        resp.setApprovedAt(r.getApprovedAt());
        resp.setPublishedBy(r.getPublishedBy());
        resp.setPublishedAt(r.getPublishedAt());
        resp.setAllowedTransitions(r.getStatus().getAllowedTransitions().stream()
            .map(RatingResultStatus::name).collect(Collectors.toList()));
        return resp;
    }
}
