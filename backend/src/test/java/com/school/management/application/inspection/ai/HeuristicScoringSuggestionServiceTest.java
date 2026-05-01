package com.school.management.application.inspection.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Heuristic AI 评分建议")
class HeuristicScoringSuggestionServiceTest {

    HeuristicScoringSuggestionService svc = new HeuristicScoringSuggestionService();

    @Nested
    @DisplayName("SCORE 模式")
    class ScoreModeTests {
        @Test
        @DisplayName("无问题词 → 建议满分")
        void shouldSuggestFullScoreWhenNoIssues() {
            var resp = svc.suggest(new ScoringSuggestionService.SuggestScoreRequest(
                    "现场整洁有序, 学生纪律良好", "卫生检查", 100, "SCORE", null));
            assertThat(resp.suggestedScore()).isEqualTo(100);
            assertThat(resp.confidence()).isGreaterThan(0.5);
            assertThat(resp.provider()).isEqualTo("heuristic");
        }

        @Test
        @DisplayName("严重问题词 → 建议 20% 分")
        void shouldSuggestLowScoreOnSevere() {
            var resp = svc.suggest(new ScoringSuggestionService.SuggestScoreRequest(
                    "存在严重卫生不合格问题, 多处脏污", "卫生检查", 100, "SCORE", null));
            assertThat(resp.suggestedScore()).isEqualTo(20);
            assertThat(resp.categoryTags()).contains("卫生");
        }

        @Test
        @DisplayName("中度问题词 → 建议 55%")
        void shouldSuggestMidScoreOnModerate() {
            var resp = svc.suggest(new ScoringSuggestionService.SuggestScoreRequest(
                    "操作较差, 步骤缺失", "操作流程", 100, "SCORE", null));
            assertThat(resp.suggestedScore()).isEqualTo(55);
        }

        @Test
        @DisplayName("空描述 → NONE 级别 → 建议满分")
        void shouldHandleBlankDescription() {
            var resp = svc.suggest(new ScoringSuggestionService.SuggestScoreRequest(
                    "", "X", 100, "SCORE", null));
            assertThat(resp.suggestedScore()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("PASS_FAIL 模式")
    class PassFailTests {
        @Test
        @DisplayName("严重问题 → FAIL 高置信度")
        void shouldVerdictFailForSevere() {
            var resp = svc.suggest(new ScoringSuggestionService.SuggestScoreRequest(
                    "存在重大安全隐患, 危险", "安全", 1, "PASS_FAIL", null));
            assertThat(resp.suggestedVerdict()).isEqualTo("FAIL");
            assertThat(resp.confidence()).isGreaterThan(0.8);
            assertThat(resp.categoryTags()).contains("安全");
        }

        @Test
        @DisplayName("正常描述 → PASS")
        void shouldVerdictPassForNormal() {
            var resp = svc.suggest(new ScoringSuggestionService.SuggestScoreRequest(
                    "运行正常, 无异常", "设备", 1, "PASS_FAIL", null));
            assertThat(resp.suggestedVerdict()).isEqualTo("PASS");
        }
    }

    @Nested
    @DisplayName("DEDUCTION 模式")
    class DeductionTests {
        @Test
        @DisplayName("严重问题 → 扣满分")
        void shouldDeductFullForSevere() {
            var resp = svc.suggest(new ScoringSuggestionService.SuggestScoreRequest(
                    "设施严重损坏, 故障无法使用", "设施", 50, "DEDUCTION", null));
            assertThat(resp.suggestedScore()).isEqualTo(50);
            assertThat(resp.categoryTags()).contains("设施");
        }

        @Test
        @DisplayName("无问题 → 不扣分")
        void shouldDeductZeroForNone() {
            var resp = svc.suggest(new ScoringSuggestionService.SuggestScoreRequest(
                    "运转良好", "X", 50, "DEDUCTION", null));
            assertThat(resp.suggestedScore()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("类目识别")
    class CategoryTests {
        @Test
        @DisplayName("多个类目同时命中")
        void shouldDetectMultipleCategories() {
            var resp = svc.suggest(new ScoringSuggestionService.SuggestScoreRequest(
                    "卫生差, 又有安全隐患, 学生还有迟到", "X", 100, "SCORE", null));
            assertThat(resp.categoryTags()).contains("卫生", "安全", "纪律");
        }

        @Test
        @DisplayName("无关键词时类目为空")
        void shouldReturnEmptyCategoriesWhenNoMatch() {
            var resp = svc.suggest(new ScoringSuggestionService.SuggestScoreRequest(
                    "abc 123 没有具体描述", "X", 100, "SCORE", null));
            assertThat(resp.categoryTags()).isEmpty();
        }
    }

    @Nested
    @DisplayName("provider 标识")
    class ProviderTests {
        @Test
        @DisplayName("provider 始终为 heuristic")
        void shouldReturnHeuristicProvider() {
            assertThat(svc.providerName()).isEqualTo("heuristic");
            var resp = svc.suggest(new ScoringSuggestionService.SuggestScoreRequest(
                    "X", "X", 100, "SCORE", List.of()));
            assertThat(resp.provider()).isEqualTo("heuristic");
        }
    }
}
