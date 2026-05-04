package com.school.management.application.inspection.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek API 实现 — OpenAI 兼容协议, 单 token 流式 chat completions.
 *
 * <p>启用条件: `inspection.ai.provider=deepseek` 且 `inspection.ai.deepseek.api-key` 非空.
 * 否则保留 Heuristic 单例.
 *
 * <p>失败自动回退到 Heuristic — 网络/API 异常不影响检查员体验.
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "inspection.ai.provider", havingValue = "deepseek")
public class DeepSeekScoringSuggestionService implements ScoringSuggestionService {

    private static final String SYSTEM_PROMPT = """
            你是一个检查/巡查辅助打分助手. 根据检查员的现场观察描述, 给出建议分数和扣分理由.
            返回 JSON, 仅含字段: suggestedScore (整数), suggestedVerdict (PASS/FAIL/null),
            categoryTags (字符串数组, 如 ["卫生","安全"]), confidence (0-1 小数), reasoning (中文 1-2 句).
            不要返回任何 JSON 外的内容.
            """;

    private final HeuristicScoringSuggestionService fallback;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final long timeoutMs;

    public DeepSeekScoringSuggestionService(
            HeuristicScoringSuggestionService fallback,
            ObjectMapper objectMapper,
            @Value("${inspection.ai.deepseek.api-key:}") String apiKey,
            @Value("${inspection.ai.deepseek.api-url:https://api.deepseek.com/chat/completions}") String apiUrl,
            @Value("${inspection.ai.deepseek.model:deepseek-chat}") String model,
            @Value("${inspection.ai.deepseek.timeout-ms:15000}") long timeoutMs) {
        this.fallback = fallback;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.timeoutMs = timeoutMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.min(timeoutMs, 5000)))
                .build();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DeepSeek provider 已选, 但 inspection.ai.deepseek.api-key 为空 — 实际调用将回退到 Heuristic");
        } else {
            log.info("DeepSeek provider 启用, model={}, url={}", model, apiUrl);
        }
    }

    @Override
    public SuggestScoreResponse suggest(SuggestScoreRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallback.suggest(request);
        }
        try {
            String userPrompt = buildUserPrompt(request);
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "response_format", Map.of("type", "json_object"),
                    "temperature", 0.3,
                    "max_tokens", 400
            ));

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                log.warn("DeepSeek HTTP {} — 回退 Heuristic. body={}", resp.statusCode(),
                        resp.body() != null ? resp.body().substring(0, Math.min(200, resp.body().length())) : "");
                return fallback.suggest(request);
            }
            return parseResponse(resp.body(), request);
        } catch (Exception e) {
            log.warn("DeepSeek 调用失败 — 回退 Heuristic: {}", e.getMessage());
            return fallback.suggest(request);
        }
    }

    @Override
    public String providerName() {
        return "deepseek";
    }

    private String buildUserPrompt(SuggestScoreRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("评分项: ").append(req.itemTitle() != null ? req.itemTitle() : "(未指定)").append("\n");
        sb.append("满分: ").append(req.itemMaxScore() != null ? req.itemMaxScore() : 100).append("\n");
        sb.append("评分模式: ").append(req.scoringMode() != null ? req.scoringMode() : "SCORE").append("\n");
        sb.append("现场观察:\n").append(req.description()).append("\n");
        if (req.evidenceUrls() != null && !req.evidenceUrls().isEmpty()) {
            sb.append("证据: ").append(String.join(", ", req.evidenceUrls())).append("\n");
        }
        sb.append("\n请给出 JSON 评分建议.");
        return sb.toString();
    }

    private SuggestScoreResponse parseResponse(String body, SuggestScoreRequest req) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                log.warn("DeepSeek 返回空 content — 回退 Heuristic");
                return fallback.suggest(req);
            }
            JsonNode parsed = objectMapper.readTree(content);

            Integer suggestedScore = parsed.has("suggestedScore") && !parsed.get("suggestedScore").isNull()
                    ? parsed.get("suggestedScore").asInt() : null;
            String suggestedVerdict = parsed.has("suggestedVerdict") && !parsed.get("suggestedVerdict").isNull()
                    ? parsed.get("suggestedVerdict").asText() : null;
            double confidence = parsed.has("confidence") ? parsed.get("confidence").asDouble(0.7) : 0.7;
            String reasoning = parsed.has("reasoning") ? parsed.get("reasoning").asText() : "";

            List<String> tags = new ArrayList<>();
            if (parsed.has("categoryTags") && parsed.get("categoryTags").isArray()) {
                for (JsonNode n : parsed.get("categoryTags")) tags.add(n.asText());
            }

            log.debug("DeepSeek suggestion: score={}, verdict={}, conf={}", suggestedScore, suggestedVerdict, confidence);
            return new SuggestScoreResponse(suggestedScore, suggestedVerdict, tags, confidence, reasoning, providerName());
        } catch (Exception e) {
            log.warn("DeepSeek 响应解析失败 — 回退 Heuristic: {}", e.getMessage());
            return fallback.suggest(req);
        }
    }
}
