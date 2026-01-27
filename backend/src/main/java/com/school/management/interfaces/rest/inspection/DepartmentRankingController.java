package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.DepartmentRankingQueryService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.DepartmentResultStats;
import com.school.management.interfaces.rest.inspection.dto.DepartmentRankingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for department ranking queries.
 */
@RestController
@RequestMapping("/inspection/department-ranking")
@Tag(name = "Department Ranking", description = "Department ranking query API")
public class DepartmentRankingController {

    private final DepartmentRankingQueryService rankingService;

    public DepartmentRankingController(DepartmentRankingQueryService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    @Operation(summary = "Get department ranking for a date range")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<DepartmentRankingResponse>> getRanking(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1.0") BigDecimal classWeight) {

        List<DepartmentRankingResponse> ranking = rankingService.getDepartmentRanking(startDate, endDate, classWeight)
            .stream()
            .map(DepartmentRankingResponse::fromDomain)
            .collect(Collectors.toList());
        return Result.success(ranking);
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get department ranking for a single session")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<DepartmentRankingResponse>> getRankingBySession(@PathVariable Long sessionId) {
        List<DepartmentRankingResponse> ranking = rankingService.getDepartmentRankingBySession(sessionId)
            .stream()
            .map(DepartmentRankingResponse::fromDomain)
            .collect(Collectors.toList());
        return Result.success(ranking);
    }
}
