package com.school.management.application.inspection.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 启发式 AI 辅助打分实现 — 基于关键词权重, 无需任何外部 API.
 *
 * <p>规则 (可后续扩展):
 *   - 描述中含"严重 / 不合格 / 危险 / 故障 / 损坏" → 严重扣分 (建议 0%-30%)
 *   - 含"较差 / 不规范 / 缺失 / 未完成" → 中度扣分 (40%-60%)
 *   - 含"基本合格 / 一般 / 部分" → 轻度扣分 (70%-85%)
 *   - 仅正面词 / 无负面词 → 满分或近满分 (95%-100%)
 *
 * <p>类目识别: 关键词 → 标签集合, 多个类目可同时命中.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "inspection.ai.provider", havingValue = "heuristic", matchIfMissing = true)
public class HeuristicScoringSuggestionService implements ScoringSuggestionService {

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
            "卫生", List.of("卫生", "脏", "垃圾", "灰尘", "污渍", "异味"),
            "安全", List.of("安全", "危险", "隐患", "事故", "漏电", "失火"),
            "纪律", List.of("纪律", "迟到", "缺勤", "打闹", "早退", "违规"),
            "设施", List.of("设施", "损坏", "故障", "破损", "丢失", "失效"),
            "服务", List.of("服务", "态度", "回应", "礼貌", "效率")
    );

    private static final List<String> SEVERE_KEYWORDS = List.of(
            "严重", "不合格", "危险", "故障", "损坏", "事故", "失火", "漏电", "失效");
    private static final List<String> MODERATE_KEYWORDS = List.of(
            "较差", "不规范", "缺失", "未完成", "不齐", "脏乱", "迟到");
    private static final List<String> LIGHT_KEYWORDS = List.of(
            "基本合格", "一般", "部分", "略", "稍");

    @Override
    public SuggestScoreResponse suggest(SuggestScoreRequest request) {
        String desc = request.description() == null ? "" : request.description().toLowerCase();
        Integer max = request.itemMaxScore() != null ? request.itemMaxScore() : 100;
        String mode = request.scoringMode() != null ? request.scoringMode() : "SCORE";

        // 1) 命中等级判定
        Severity severity = detectSeverity(desc);

        // 2) 类目识别
        List<String> categoryTags = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            for (String kw : entry.getValue()) {
                if (desc.contains(kw)) {
                    categoryTags.add(entry.getKey());
                    break;
                }
            }
        }

        // 3) 按 mode 给出建议
        Integer suggestedScore = null;
        String suggestedVerdict = null;
        String reasoning;
        double confidence;

        switch (mode) {
            case "PASS_FAIL" -> {
                suggestedVerdict = severity == Severity.SEVERE || severity == Severity.MODERATE ? "FAIL" : "PASS";
                reasoning = severity == Severity.SEVERE
                        ? "描述包含严重问题词, 建议判定不通过."
                        : severity == Severity.MODERATE
                            ? "描述含中度问题词, 建议判定不通过, 请检查员复核."
                            : "未检测到明显问题词, 建议判定通过.";
                confidence = severity == Severity.SEVERE ? 0.85 : severity == Severity.MODERATE ? 0.65 : 0.6;
            }
            case "DEDUCTION" -> {
                int deduction = switch (severity) {
                    case SEVERE -> max;
                    case MODERATE -> (int) (max * 0.5);
                    case LIGHT -> (int) (max * 0.2);
                    case NONE -> 0;
                };
                suggestedScore = deduction;
                reasoning = severity == Severity.NONE
                        ? "未检测到问题词, 建议不扣分."
                        : "检测到 " + severity.cn + " 级别问题, 建议扣 " + deduction + " 分.";
                confidence = severity == Severity.NONE ? 0.7 : 0.6;
            }
            default -> {
                // SCORE 模式: 给出建议分数 (满分基础上扣)
                int score = switch (severity) {
                    case SEVERE -> Math.max(0, (int) (max * 0.2));
                    case MODERATE -> (int) (max * 0.55);
                    case LIGHT -> (int) (max * 0.8);
                    case NONE -> max;
                };
                suggestedScore = score;
                reasoning = severity == Severity.NONE
                        ? "未检测到明显问题, 建议满分."
                        : "检测到 " + severity.cn + " 级别问题, 建议给 " + score + "/" + max + " 分.";
                confidence = severity == Severity.NONE ? 0.6 : 0.65;
            }
        }

        log.debug("Heuristic suggestion: severity={}, score={}, verdict={}, tags={}, conf={}",
                severity, suggestedScore, suggestedVerdict, categoryTags, confidence);

        return new SuggestScoreResponse(
                suggestedScore, suggestedVerdict,
                categoryTags, confidence, reasoning, providerName());
    }

    @Override
    public String providerName() {
        return "heuristic";
    }

    private Severity detectSeverity(String desc) {
        if (desc.isBlank()) return Severity.NONE;
        for (String kw : SEVERE_KEYWORDS) if (desc.contains(kw)) return Severity.SEVERE;
        for (String kw : MODERATE_KEYWORDS) if (desc.contains(kw)) return Severity.MODERATE;
        for (String kw : LIGHT_KEYWORDS) if (desc.contains(kw)) return Severity.LIGHT;
        return Severity.NONE;
    }

    private enum Severity {
        NONE("无"), LIGHT("轻度"), MODERATE("中度"), SEVERE("严重");
        final String cn;
        Severity(String cn) { this.cn = cn; }
    }
}
