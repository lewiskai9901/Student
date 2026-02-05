package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.OrgRankingQueryService;
import com.school.management.common.result.Result;
import com.school.management.interfaces.rest.inspection.dto.OrgRankingResponse;
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
 * 组织排名 REST 控制器
 */
@RestController
@RequestMapping("/inspection/org-ranking")
@Tag(name = "Organization Ranking", description = "组织排名查询 API")
public class OrgRankingController {

    private final OrgRankingQueryService rankingService;

    public OrgRankingController(OrgRankingQueryService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    @Operation(summary = "获取日期范围内的组织排名")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<OrgRankingResponse>> getRanking(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1.0") BigDecimal classWeight) {

        List<OrgRankingResponse> ranking = rankingService.getOrgRanking(startDate, endDate, classWeight)
            .stream()
            .map(OrgRankingResponse::fromDomain)
            .collect(Collectors.toList());
        return Result.success(ranking);
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "获取单个检查场次的组织排名")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<OrgRankingResponse>> getRankingBySession(@PathVariable Long sessionId) {
        List<OrgRankingResponse> ranking = rankingService.getOrgRankingBySession(sessionId)
            .stream()
            .map(OrgRankingResponse::fromDomain)
            .collect(Collectors.toList());
        return Result.success(ranking);
    }
}
