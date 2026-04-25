package com.school.management.domain.inspection.model.execution;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 评分观察 — 评分引擎的归一化输出.
 *
 * <p>不管原始检查项是什么类型(通过不通过/打分/违规记录/人次扣分...),
 * 评分后都归一化为 0~N 个 ScoringObservation.
 *
 * <p>一个 ScoringObservation = 对一个主体的一次评分观察.
 */
@Getter
@Builder
public class ScoringObservation {

    // ── 主体 ──
    private final String subjectType;       // USER / ORG_UNIT / PLACE / ASSET
    private final Long subjectId;
    private final String subjectName;
    private final Long orgUnitId;           // 主体所属组织 (冗余, 加速查询)
    private final String orgUnitName;

    // ── 评分结果 ──
    private final BigDecimal score;
    private final boolean negative;         // score < 0 || flagged
    private final String severity;          // LOW / MEDIUM / HIGH / CRITICAL
    private final boolean flagged;

    // ── 事件关联 ──
    private final String linkedEventType;   // 从 templateItem.linked_event_type_code

    // ── 检查项信息 ──
    private final String itemCode;
    private final String itemName;
    private final String itemType;
    private final String sectionName;

    // ── 原始数据 ──
    private final String responseValue;
    private final String description;
    private final LocalDateTime observedAt;

    // ── 关联 ID (持久化用) ──
    private final Long submissionId;
    private final Long detailId;
    private final Long projectId;
    private final Long taskId;

    /**
     * 转换为 TriggerService.fire() 的 context Map.
     * <p>使用通用键约定: subjectUserId/subjectUserName/orgUnitId/orgUnitName/placeId/placeName.
     * 行业特定的"学生/班级"等语义由下游订阅规则负责映射.
     */
    public Map<String, Object> toContextMap() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("isNegative", negative);
        ctx.put("severity", severity);
        if (linkedEventType != null) ctx.put("eventTypeHint", linkedEventType);

        // 主体字段 — 按 subjectType 映射到通用键
        if ("USER".equals(subjectType)) {
            ctx.put("subjectUserId", subjectId);
            ctx.put("subjectUserName", subjectName != null ? subjectName : "");
        } else if ("PLACE".equals(subjectType)) {
            ctx.put("placeId", subjectId);
            ctx.put("placeName", subjectName != null ? subjectName : "");
        }
        // 主体所属组织 (跨主体类型都可能有)
        if (orgUnitId != null) {
            ctx.put("orgUnitId", orgUnitId);
            ctx.put("orgUnitName", orgUnitName != null ? orgUnitName : "");
        }

        ctx.put("itemName", itemName != null ? itemName : "");
        ctx.put("score", score);
        if (description != null) ctx.put("description", description);
        return ctx;
    }

    // ── Severity 计算工具 ──

    public static String calcSeverity(BigDecimal score, boolean isFlagged) {
        if (isFlagged && (score == null || score.compareTo(BigDecimal.ZERO) == 0)) return "MEDIUM";
        if (score == null) return "LOW";
        double abs = score.abs().doubleValue();
        if (abs >= 10) return "CRITICAL";
        if (abs >= 5) return "HIGH";
        if (abs >= 2) return "MEDIUM";
        return "LOW";
    }

    public static String mapViolationSeverity(String severity) {
        if (severity == null) return "MEDIUM";
        return switch (severity.toUpperCase()) {
            case "SEVERE" -> "CRITICAL";
            case "MODERATE" -> "HIGH";
            case "MINOR" -> "MEDIUM";
            default -> "LOW";
        };
    }
}
