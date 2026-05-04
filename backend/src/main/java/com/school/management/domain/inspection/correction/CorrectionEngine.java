package com.school.management.domain.inspection.correction;

import com.school.management.domain.inspection.model.execution.ScoringMode;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 整改判定引擎 — 4 层决策:
 *  L1 SeverityNormalizer (mode 标准化为 [0,1])
 *  L2 ItemRule (检查项级覆盖, Sprint 1 暂跳过, 走默认)
 *  L3 ProjectPolicy.thresholds → Severity
 *  L4 复发增强 (Sprint 2 引入, 此处接口预留)
 *
 * <p>引擎是无状态的, 每次 judge 输入一个 detail + policy + 历史复发计数.
 */
@Service
public class CorrectionEngine {

    private static final Logger log = LoggerFactory.getLogger(CorrectionEngine.class);

    /**
     * 单条判定.
     * @param detail 提交明细
     * @param policy 项目策略
     * @param recurrenceCount 近 30 天该 itemCode + subject 出现次数 (0 表示首次)
     */
    public CorrectionVerdict judge(SubmissionDetail detail,
                                   ProjectCorrectivePolicy policy,
                                   int recurrenceCount) {
        if (policy == null) policy = ProjectCorrectivePolicy.normalDefault();

        CorrectionVerdict.Builder b = CorrectionVerdict.builder()
                .detailId(detail.getId())
                .itemCode(detail.getItemCode())
                .itemName(detail.getItemName());

        // OFF 模式: 引擎完全不建议
        if (policy.isOff()) {
            return b.severity(Severity.NONE)
                    .reason("项目策略为 OFF, 完全人工")
                    .addTrace("policy", "OFF", "—", "skip")
                    .build();
        }

        // L1 标准化
        ScoringMode mode = detail.getScoringMode();
        SeverityNormalizer normalizer = SeverityNormalizer.of(mode);
        Double sev = normalizer.normalize(detail, detail.getItemWeight());
        if (sev == null) {
            return b.severity(Severity.NONE)
                    .reason("响应未参与判定 (空/N/A)")
                    .addTrace("normalize", String.valueOf(mode), "skip", "null")
                    .build();
        }
        b.severityScore(sev)
         .addTrace("normalize", String.valueOf(mode),
                 String.format("score=%s/weight=%s/resp=%s",
                         detail.getScore(), detail.getItemWeight(), detail.getResponseValue()),
                 String.format("%.3f", sev));

        // L3 项目阈值 → severity 等级
        SeverityThresholds t = policy.thresholds();
        Severity level = t.classify(sev);
        b.addTrace("threshold", policy.strictness(),
                String.format("h=%.2f m=%.2f l=%.2f", t.high(), t.medium(), t.low()),
                level.name());

        // L4 复发增强
        if (recurrenceCount >= 1 && level.requiresCorrection()) {
            Severity escalated = level.escalateOne();
            if (escalated != level) {
                b.addTrace("recurrence", "30d_count=" + recurrenceCount,
                        level.name(), escalated.name() + " (升级)");
                level = escalated;
            }
        }

        // mustCorrect: HIGH 必须 / MEDIUM 项目级可关闭 / LOW 建议
        boolean must = level == Severity.HIGH;
        if (level == Severity.MEDIUM && "STRICT".equalsIgnoreCase(policy.strictness())) {
            must = true;
        }
        // 复发 ≥3 次强制建单
        if (recurrenceCount >= 3 && level.requiresCorrection()) must = true;

        int days = policy.deadlines().forSeverity(level);

        return b.severity(level)
                .mustCorrect(must)
                .deadlineDays(days)
                .reason(buildReason(detail, mode, sev, level, recurrenceCount))
                .build();
    }

    /** 批量判定. */
    public List<CorrectionVerdict> judgeAll(List<SubmissionDetail> details,
                                            ProjectCorrectivePolicy policy,
                                            RecurrenceLookup lookup) {
        List<CorrectionVerdict> verdicts = new ArrayList<>();
        for (SubmissionDetail d : details) {
            int rc = lookup == null ? 0 : lookup.countRecent(d);
            try {
                verdicts.add(judge(d, policy, rc));
            } catch (Exception ex) {
                log.warn("judge detail {} failed: {}", d.getId(), ex.getMessage());
            }
        }
        return verdicts;
    }

    private String buildReason(SubmissionDetail d, ScoringMode mode, double sev,
                               Severity level, int recur) {
        StringBuilder sb = new StringBuilder();
        sb.append(d.getItemName()).append(": ");
        switch (mode == null ? ScoringMode.DEDUCTION : mode) {
            case PASS_FAIL:
                sb.append("未通过");
                break;
            case LEVEL:
                sb.append("等级 ").append(d.getResponseValue());
                break;
            case RATING_SCALE:
                sb.append("评分 ").append(d.getResponseValue()).append(" 偏低");
                break;
            case DEDUCTION:
            case TIERED_DEDUCTION:
            case CUMULATIVE:
                sb.append(String.format("扣分占比 %.0f%%", sev * 100));
                break;
            default:
                sb.append(String.format("严重度 %.0f%%", sev * 100));
        }
        if (recur >= 1) sb.append(" / 近 30 天复发 ").append(recur).append(" 次");
        sb.append(" → ").append(level.name());
        return sb.toString();
    }

    /** 复发计数查询接口 (基础设施层实现). */
    @FunctionalInterface
    public interface RecurrenceLookup {
        int countRecent(SubmissionDetail detail);
    }
}
