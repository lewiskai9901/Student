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
        return judge(detail, policy, ItemRule.EMPTY, recurrenceCount);
    }

    /**
     * 单条判定 (含 ItemRule 检查项覆盖).
     */
    public CorrectionVerdict judge(SubmissionDetail detail,
                                   ProjectCorrectivePolicy policy,
                                   ItemRule itemRule,
                                   int recurrenceCount) {
        if (policy == null) policy = ProjectCorrectivePolicy.normalDefault();
        if (itemRule == null) itemRule = ItemRule.EMPTY;

        CorrectionVerdict.Builder b = CorrectionVerdict.builder()
                .detailId(detail.getId())
                .itemCode(detail.getItemCode())
                .itemName(detail.getItemName());

        // Step 1: 总开关
        if (policy.isOff()) {
            return b.severity(Severity.NONE)
                    .reason("项目整改引擎已关闭, 完全人工")
                    .addTrace("policy", "disabled", "—", "skip")
                    .build();
        }

        // Step 2: 题目级 neverCorrect
        if (itemRule.isNeverCorrect()) {
            return b.severity(Severity.NONE)
                    .reason("检查项配置 neverCorrect=true")
                    .addTrace("itemRule", "neverCorrect", "—", "skip")
                    .build();
        }

        // Step 3: baseSeverity 来源 (优先级: baseSeverityMap > singleThreshold > normalizer + 项目阈值)
        // baseSeverityMap 同时尝试: 响应值原值 + normalizer 解析的"语义标签" (用于 RISK_MATRIX 等复合模式)
        Severity baseSev;
        double sevScore = 0.0;
        ScoringMode dmode = detail.getScoringMode();
        SeverityNormalizer dnormalizer = SeverityNormalizer.of(dmode);
        Severity explicit = itemRule.lookupBaseSeverity(detail.getResponseValue());
        String semanticLabel = null;
        if (explicit == null) {
            // fallback: 用 normalizer 解析的语义标签 (例如 RISK_MATRIX 的 L/M/H/VH)
            semanticLabel = dnormalizer.resolveLabel(detail);
            if (semanticLabel != null && !semanticLabel.equals(detail.getResponseValue())) {
                explicit = itemRule.lookupBaseSeverity(semanticLabel);
            }
        }
        if (explicit != null) {
            baseSev = explicit;
            sevScore = explicit == Severity.HIGH ? 1.0 : explicit == Severity.MEDIUM ? 0.6 : explicit == Severity.LOW ? 0.3 : 0.0;
            String matchKey = semanticLabel != null && itemRule.lookupBaseSeverity(detail.getResponseValue()) == null
                    ? "label=" + semanticLabel
                    : "resp=" + detail.getResponseValue();
            b.severityScore(sevScore)
             .addTrace("itemRule.baseSeverityMap",
                     matchKey, "—", explicit.name());
        } else if (itemRule.hasSingleThreshold()) {
            // 题目级单阈值: sev ≥ sevThreshold → triggerSeverity, 否则 NONE
            ScoringMode mode = detail.getScoringMode();
            SeverityNormalizer normalizer = SeverityNormalizer.of(mode);
            Double sev = normalizer.normalize(detail, detail.getItemWeight());
            if (sev == null) {
                return b.severity(Severity.NONE)
                        .reason("响应未参与判定 (空/N/A)")
                        .addTrace("normalize", String.valueOf(mode), "skip", "null")
                        .build();
            }
            sevScore = sev;
            ItemRule.SingleThreshold st = itemRule.getSingleThreshold();
            if (sev >= st.getSevThreshold()) {
                baseSev = st.getTriggerSeverity();
                b.severityScore(sev)
                 .addTrace("itemRule.singleThreshold",
                         String.format("sev=%.3f ≥ %.3f", sev, st.getSevThreshold()),
                         "—", baseSev.name());
            } else {
                baseSev = Severity.NONE;
                b.severityScore(sev)
                 .addTrace("itemRule.singleThreshold",
                         String.format("sev=%.3f < %.3f", sev, st.getSevThreshold()),
                         "—", "NONE");
            }
        } else {
            ScoringMode mode = detail.getScoringMode();
            SeverityNormalizer normalizer = SeverityNormalizer.of(mode);
            Double sev = normalizer.normalize(detail, detail.getItemWeight());
            if (sev == null) {
                return b.severity(Severity.NONE)
                        .reason("响应未参与判定 (空/N/A)")
                        .addTrace("normalize", String.valueOf(mode), "skip", "null")
                        .build();
            }
            sevScore = sev;
            b.severityScore(sev)
             .addTrace("normalize", String.valueOf(mode),
                     String.format("score=%s/weight=%s/resp=%s",
                             detail.getScore(), detail.getItemWeight(), detail.getResponseValue()),
                     String.format("%.3f", sev));
            baseSev = policy.thresholds().classify(sev);
            b.addTrace("threshold", "project",
                    String.format("h=%.2f m=%.2f l=%.2f",
                            policy.thresholds().high(), policy.thresholds().medium(), policy.thresholds().low()),
                    baseSev.name());
        }

        // Step 4: 红线题强制 HIGH
        if (itemRule.isRedLine() && baseSev != Severity.NONE) {
            b.addTrace("itemRule.criticality", "RED", baseSev.name(), "HIGH (forced)");
            baseSev = Severity.HIGH;
        }

        // Step 5: 复发增强 (近 30 天 ≥1 次 → 升级一档; ≥3 次额外升级 + mustCorrect)
        Severity afterRecurrence = baseSev;
        if (recurrenceCount >= 1 && baseSev.requiresCorrection()) {
            afterRecurrence = baseSev.escalateOne();
            if (afterRecurrence != baseSev) {
                b.addTrace("recurrence", "30d=" + recurrenceCount, baseSev.name(), afterRecurrence.name());
            }
        }

        // Step 6: 项目级整体调档 (strictnessAdjustment)
        Severity finalSev = policy.shiftBy(afterRecurrence);
        if (finalSev != afterRecurrence) {
            b.addTrace("strictnessAdj", "adj=" + policy.strictnessAdjustment(),
                    afterRecurrence.name(), finalSev.name());
        }

        // Step 7: 自动建单门槛 (policy.autoCreateLevel 判定)
        boolean must = policy.shouldAutoCreate(finalSev);
        // 复发 ≥3 次 + 需整改 → 强制 mustCorrect (避免被 autoCreateLevel=NONE 漏掉严重屡犯)
        if (recurrenceCount >= 3 && finalSev.requiresCorrection()) must = true;

        // Step 8: deadline (itemRule.deadlineOverrideDays 优先, 否则项目 preset)
        int days;
        if (itemRule.getDeadlineOverrideDays() != null) {
            days = itemRule.getDeadlineOverrideDays();
            b.addTrace("deadline", "itemRule.override", "—", days + "d");
        } else {
            days = policy.deadlines().forSeverity(finalSev);
        }

        return b.severity(finalSev)
                .mustCorrect(must)
                .deadlineDays(days)
                .reason(buildReason(detail, detail.getScoringMode(), sevScore, finalSev, recurrenceCount))
                .build();
    }

    /** 批量判定. */
    public List<CorrectionVerdict> judgeAll(List<SubmissionDetail> details,
                                            ProjectCorrectivePolicy policy,
                                            RecurrenceLookup lookup) {
        return judgeAll(details, policy, lookup, ItemRuleLookup.NONE);
    }

    /** 批量判定 (含 ItemRule 查询). */
    public List<CorrectionVerdict> judgeAll(List<SubmissionDetail> details,
                                            ProjectCorrectivePolicy policy,
                                            RecurrenceLookup lookup,
                                            ItemRuleLookup itemLookup) {
        List<CorrectionVerdict> verdicts = new ArrayList<>();
        for (SubmissionDetail d : details) {
            int rc = lookup == null ? 0 : lookup.countRecent(d);
            ItemRule rule = itemLookup == null ? ItemRule.EMPTY : itemLookup.lookup(d);
            try {
                verdicts.add(judge(d, policy, rule, rc));
            } catch (Exception ex) {
                // P2#14: 单条失败不再仅 warn 后丢弃 — 那样 verdicts 列表会比 details 短,
                // 调用方按下标对齐时错位且无感. 改为塞一个 NONE 占位 verdict, trace 记录失败原因,
                // 列表长度与 details 一一对应, 调用方可据 trace 识别失败明细.
                log.warn("judge detail {} failed: {}", d.getId(), ex.getMessage());
                verdicts.add(CorrectionVerdict.builder()
                        .detailId(d.getId())
                        .itemCode(d.getItemCode())
                        .itemName(d.getItemName())
                        .severity(Severity.NONE)
                        .reason("判定失败, 已跳过: " + ex.getMessage())
                        .addTrace("error", ex.getClass().getSimpleName(),
                                String.valueOf(ex.getMessage()), "NONE (placeholder)")
                        .build());
            }
        }
        return verdicts;
    }

    /** 检查项级 ItemRule 查询接口. */
    @FunctionalInterface
    public interface ItemRuleLookup {
        ItemRuleLookup NONE = d -> ItemRule.EMPTY;
        ItemRule lookup(SubmissionDetail detail);
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
