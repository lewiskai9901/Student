package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.InspRatingCalculationHandler;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.rating.InspRatingLink;
import com.school.management.domain.inspection.repository.v7.InspRatingLinkRepository;
import com.school.management.exception.BusinessException;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v7/insp/rating-links")
@RequiredArgsConstructor
public class InspRatingLinkController {

    private final InspRatingLinkRepository linkRepository;
    private final InspRatingCalculationHandler calculationHandler;

    @GetMapping
    @CasbinAccess(resource = "insp:rating", action = "view")
    public Result<List<InspRatingLink>> getByProject(@RequestParam Long projectId) {
        return Result.success(linkRepository.findByProjectId(projectId));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:rating", action = "view")
    public Result<InspRatingLink> getById(@PathVariable Long id) {
        return Result.success(linkRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Rating link not found: " + id)));
    }

    @PostMapping
    @CasbinAccess(resource = "insp:rating", action = "manage")
    public Result<InspRatingLink> create(@RequestBody CreateLinkRequest request) {
        InspRatingLink link = InspRatingLink.create(
                request.getProjectId(),
                request.getRatingConfigId(),
                request.getPeriodType(),
                request.isAutoCalculate(),
                request.getCreatedBy()
        );
        return Result.success(linkRepository.save(link));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:rating", action = "manage")
    public Result<InspRatingLink> update(@PathVariable Long id, @RequestBody UpdateLinkRequest request) {
        InspRatingLink link = linkRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Rating link not found: " + id));
        link.update(request.getPeriodType(), request.isAutoCalculate());
        return Result.success(linkRepository.save(link));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:rating", action = "manage")
    public Result<Void> delete(@PathVariable Long id) {
        linkRepository.deleteById(id);
        return Result.success(null);
    }

    @PostMapping("/calculate")
    @CasbinAccess(resource = "insp:rating", action = "manage")
    public Result<Void> manualCalculate(@RequestBody CalculateRequest request) {
        calculationHandler.calculateRatings(
                request.getProjectId(),
                request.getPeriodType(),
                request.getPeriodStart(),
                request.getPeriodEnd()
        );
        return Result.success(null);
    }

    // ========== Request DTOs ==========

    @lombok.Data
    public static class CreateLinkRequest {
        private Long projectId;
        private Long ratingConfigId;
        private String periodType;
        private boolean autoCalculate = true;
        private Long createdBy;
    }

    @lombok.Data
    public static class UpdateLinkRequest {
        private String periodType;
        private boolean autoCalculate;
    }

    @lombok.Data
    public static class CalculateRequest {
        private Long projectId;
        private String periodType;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate periodStart;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate periodEnd;
    }
}
