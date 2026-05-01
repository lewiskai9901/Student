package com.school.management.application.inspection.ai;

import java.util.List;

/**
 * AI 辅助打分建议 — 检查员现场提交描述/证据后, 由 LLM 或启发式规则返回评分建议.
 *
 * 当前提供两种实现:
 *   - {@link HeuristicScoringSuggestionService}: 本地关键词启发式, 无需任何外部凭证, 默认启用
 *   - {@link ClaudeScoringSuggestionService}:    调 Anthropic Claude API, 需 ai.provider=claude + 环境变量
 *
 * 通过 application.yml 中 `inspection.ai.provider` 选择实现 (默认 heuristic).
 */
public interface ScoringSuggestionService {

    /**
     * 根据描述/证据返回评分建议.
     *
     * @param request 检查员观察 + 评分项上下文
     * @return 评分建议 (含建议分数 / 类目标签 / 置信度 / 理由)
     */
    SuggestScoreResponse suggest(SuggestScoreRequest request);

    /** 实现标识, 用于日志和审计 */
    String providerName();

    /** 检查员观察描述 + 评分项上下文 + 可选证据 URL */
    record SuggestScoreRequest(
            String description,           // 检查员文字观察 (必填)
            String itemTitle,             // 评分项标题
            Integer itemMaxScore,         // 评分项满分
            String scoringMode,           // SCORE / PASS_FAIL / DEDUCTION
            List<String> evidenceUrls     // 证据图片 / 视频链接 (可选)
    ) {}

    /** 返回的评分建议 */
    record SuggestScoreResponse(
            Integer suggestedScore,       // 建议分数 (SCORE / DEDUCTION 模式) — null 表示不建议数值
            String suggestedVerdict,      // PASS / FAIL (PASS_FAIL 模式) — null 表示不建议
            List<String> categoryTags,    // 问题类目标签 (例如 "卫生", "安全", "纪律")
            double confidence,            // 置信度 0.0 - 1.0
            String reasoning,             // 简短理由 (1-3 句, 给检查员看)
            String provider               // heuristic / claude
    ) {}
}
