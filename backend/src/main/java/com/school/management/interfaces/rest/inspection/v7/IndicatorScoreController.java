package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.IndicatorScoreService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.scoring.IndicatorScore;
import com.school.management.domain.inspection.repository.v7.IndicatorScoreRepository;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v7/insp/indicator-scores")
@RequiredArgsConstructor
public class IndicatorScoreController {

    private final IndicatorScoreRepository scoreRepository;
    private final IndicatorScoreService scoreService;

    @GetMapping
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<IndicatorScore>> getScores(
            @RequestParam Long indicatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        List<IndicatorScore> scores;
        if (periodStart != null && periodEnd != null) {
            scores = scoreRepository.findByIndicatorIdAndPeriod(indicatorId, periodStart, periodEnd);
        } else {
            scores = scoreRepository.findByIndicatorId(indicatorId);
        }
        return Result.success(scores);
    }

    @PostMapping("/compute")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<Void> computeScores(
            @RequestParam Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        scoreService.computeAllForProject(projectId, periodStart, periodEnd);
        return Result.success();
    }
}
