package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.ai.ScoringSuggestionService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 辅助打分 — Track 5 (Road to A).
 *
 * 检查员现场提交描述/证据后, 由 LLM 或启发式规则返回评分建议.
 */
@RestController
@RequestMapping("/inspection/ai")
@RequiredArgsConstructor
public class AiScoringController {

    private final ScoringSuggestionService scoringSuggestionService;

    @PostMapping("/suggest-score")
    @CasbinAccess(resource = "insp:task", action = "execute")
    public Result<ScoringSuggestionService.SuggestScoreResponse> suggestScore(
            @RequestBody ScoringSuggestionService.SuggestScoreRequest request) {
        if (request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException("观察描述不能为空");
        }
        return Result.success(scoringSuggestionService.suggest(request));
    }
}
