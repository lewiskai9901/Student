package com.school.management.domain.inspection.correction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 检查项级整改覆盖规则 (insp_template_items.corrective_override JSON).
 *
 * <p>所有字段都是 optional. 缺失字段 → 用项目级策略.
 * <pre>
 * {
 *   "neverCorrect": false,            // 该项永不建整改
 *   "forceCorrect": ["FAIL"],         // 特定 response 强制建单 (HIGH)
 *   "thresholdOverride": {"high":0.6,"medium":0.4,"low":0.2},
 *   "deadlineOverride": {"high":1,"medium":3,"low":7}
 * }
 * </pre>
 */
public class ItemRule {

    public static final ItemRule EMPTY = new ItemRule(false, Collections.emptyList(), null, null);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final boolean neverCorrect;
    private final List<String> forceCorrect;
    private final SeverityThresholds thresholdOverride;  // 可为 null
    private final DeadlinePresets deadlineOverride;       // 可为 null

    public ItemRule(boolean neverCorrect, List<String> forceCorrect,
                    SeverityThresholds thresholdOverride, DeadlinePresets deadlineOverride) {
        this.neverCorrect = neverCorrect;
        this.forceCorrect = forceCorrect == null ? Collections.emptyList() : forceCorrect;
        this.thresholdOverride = thresholdOverride;
        this.deadlineOverride = deadlineOverride;
    }

    public boolean isNeverCorrect() { return neverCorrect; }
    public List<String> getForceCorrect() { return forceCorrect; }
    public SeverityThresholds getThresholdOverride() { return thresholdOverride; }
    public DeadlinePresets getDeadlineOverride() { return deadlineOverride; }

    public boolean isForceCorrect(String responseValue) {
        if (responseValue == null || forceCorrect.isEmpty()) return false;
        for (String f : forceCorrect) {
            if (f != null && f.equalsIgnoreCase(responseValue)) return true;
        }
        return false;
    }

    /** 解析 JSON 到 ItemRule. 解析失败返回 EMPTY. */
    public static ItemRule fromJson(String json) {
        if (json == null || json.isBlank()) return EMPTY;
        try {
            JsonNode n = MAPPER.readTree(json);
            boolean never = n.path("neverCorrect").asBoolean(false);

            List<String> force = Collections.emptyList();
            if (n.has("forceCorrect") && n.get("forceCorrect").isArray()) {
                force = new java.util.ArrayList<>();
                for (JsonNode v : n.get("forceCorrect")) {
                    if (v.isTextual()) force.add(v.asText());
                }
            }

            SeverityThresholds tOverride = null;
            if (n.has("thresholdOverride") && n.get("thresholdOverride").isObject()) {
                JsonNode t = n.get("thresholdOverride");
                tOverride = new SeverityThresholds(
                        t.path("high").asDouble(0.8),
                        t.path("medium").asDouble(0.5),
                        t.path("low").asDouble(0.3));
            }

            DeadlinePresets dOverride = null;
            if (n.has("deadlineOverride") && n.get("deadlineOverride").isObject()) {
                JsonNode d = n.get("deadlineOverride");
                dOverride = new DeadlinePresets(
                        d.path("high").asInt(3),
                        d.path("medium").asInt(7),
                        d.path("low").asInt(14));
            }

            return new ItemRule(never, force, tOverride, dOverride);
        } catch (Exception e) {
            return EMPTY;
        }
    }

    /** 工厂方法 — 给 Map 用 (provider 解析直接给 Map). */
    @SuppressWarnings("unchecked")
    public static ItemRule fromMap(Map<String, Object> m) {
        if (m == null || m.isEmpty()) return EMPTY;
        try {
            return fromJson(MAPPER.writeValueAsString(m));
        } catch (Exception e) {
            return EMPTY;
        }
    }
}
