package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.InspRatingCalculationHandler;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.rating.InspRatingLink;
import com.school.management.domain.inspection.repository.InspRatingLinkRepository;
import com.school.management.exception.BusinessException;
import com.school.management.infrastructure.casbin.CasbinAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/inspection/rating-links")
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
    public Result<InspRatingLink> create(@RequestBody @Valid CreateLinkRequest request) {
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
    public Result<InspRatingLink> update(@PathVariable Long id, @RequestBody @Valid UpdateLinkRequest request) {
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
    public Result<Void> manualCalculate(@RequestBody @Valid CalculateRequest request) {
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
        @NotNull
        private Long projectId;
        @NotNull
        private Long ratingConfigId;
        @NotBlank
        private String periodType;
        private boolean autoCalculate = true;
        private Long createdBy;
    }

    @lombok.Data
    public static class UpdateLinkRequest {
        @NotBlank
        private String periodType;
        private boolean autoCalculate;
    }

    @lombok.Data
    public static class CalculateRequest {
        @NotNull
        private Long projectId;
        @NotBlank
        private String periodType;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate periodStart;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate periodEnd;
    }
}
