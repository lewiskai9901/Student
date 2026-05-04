package com.school.management.application.inspection.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeepSeek AI 评分建议")
class DeepSeekScoringSuggestionServiceTest {

    HeuristicScoringSuggestionService heuristic;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        heuristic = new HeuristicScoringSuggestionService();
    }

    @Nested
    @DisplayName("API key 缺失")
    class NoApiKeyTests {

        @Test
        @DisplayName("空 key 直接走 heuristic 实现 — provider 仍为 deepseek 但结果 reasoning 来自 fallback")
        void shouldFallbackWhenApiKeyMissing() {
            var svc = new DeepSeekScoringSuggestionService(
                    heuristic, objectMapper,
                    "", "https://api.deepseek.com/chat/completions",
                    "deepseek-chat", 5000);

            var resp = svc.suggest(new ScoringSuggestionService.SuggestScoreRequest(
                    "存在严重卫生不合格问题", "卫生检查", 100, "SCORE", null));

            // fallback 走的是 heuristic, provider 字段反映 heuristic
            assertThat(resp.provider()).isEqualTo("heuristic");
            assertThat(resp.suggestedScore()).isEqualTo(20);
        }

        @Test
        @DisplayName("provider 名称不受 key 影响")
        void providerNameIsAlwaysDeepseek() {
            var svc = new DeepSeekScoringSuggestionService(
                    heuristic, objectMapper,
                    "", "https://api.deepseek.com/chat/completions",
                    "deepseek-chat", 5000);
            assertThat(svc.providerName()).isEqualTo("deepseek");
        }
    }

    @Nested
    @DisplayName("无效 URL")
    class NetworkFailureTests {

        @Test
        @DisplayName("连不上的 URL → 5 秒内 fallback heuristic, 不抛异常")
        void shouldFallbackOnNetworkFailure() {
            var svc = new DeepSeekScoringSuggestionService(
                    heuristic, objectMapper,
                    "fake-key-for-test", "http://127.0.0.1:1/nonexistent",
                    "deepseek-chat", 1500);

            var resp = svc.suggest(new ScoringSuggestionService.SuggestScoreRequest(
                    "现场无问题", "纪律", 100, "SCORE", null));

            assertThat(resp).isNotNull();
            assertThat(resp.provider()).isEqualTo("heuristic");
        }
    }
}
