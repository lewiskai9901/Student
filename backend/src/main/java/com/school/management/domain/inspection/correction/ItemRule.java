package com.school.management.domain.inspection.correction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 检查项级整改规则 (insp_template_items.corrective_override JSON).
 *
 * <p>字段:
 * <ul>
 *   <li>{@code criticality} — RED 红线题, 一旦不通过强制 HIGH</li>
 *   <li>{@code neverCorrect} — 该题永不建整改 (备注/签字类)</li>
 *   <li>{@code baseSeverityMap} — 响应值 → 严重度的显式映射 (PASS_FAIL/LEVEL 离散模式)</li>
 *   <li>{@code singleThreshold} — 单阈值模式 "sev ≥ X → triggerSeverity" (RATING_SCALE/DIRECT_SCORE/DEDUCTION 连续模式)</li>
 *   <li>{@code deadlineOverrideDays} — 该题特定 deadline (天数)</li>
 * </ul>
 *
 * <p>持久化格式:
 * <pre>
 * {
 *   "criticality": "NORMAL|RED",
 *   "neverCorrect": false,
 *   "baseSeverityMap": {"FAIL":"HIGH","D":"HIGH","C":"MEDIUM"},
 *   "singleThreshold": {"sevThreshold": 0.4, "triggerSeverity": "HIGH"},
 *   "deadlineOverrideDays": 3
 * }
 * </pre>
 */
public final class ItemRule {

    public static final ItemRule EMPTY = new ItemRule(
            Criticality.NORMAL, false, Collections.emptyMap(), null, null);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public enum Criticality { NORMAL, RED }

    /** 单阈值: 题目级独立阈值, 覆盖项目阈值. */
    public static final class SingleThreshold {
        private final double sevThreshold;
        private final Severity triggerSeverity;
        public SingleThreshold(double sevThreshold, Severity triggerSeverity) {
            this.sevThreshold = sevThreshold;
            this.triggerSeverity = triggerSeverity != null ? triggerSeverity : Severity.HIGH;
        }
        public double getSevThreshold() { return sevThreshold; }
        public Severity getTriggerSeverity() { return triggerSeverity; }
    }

    private final Criticality criticality;
    private final boolean neverCorrect;
    private final Map<String, Severity> baseSeverityMap;
    private final SingleThreshold singleThreshold;
    private final Integer deadlineOverrideDays;

    public ItemRule(Criticality criticality, boolean neverCorrect,
                    Map<String, Severity> baseSeverityMap,
                    SingleThreshold singleThreshold,
                    Integer deadlineOverrideDays) {
        this.criticality = criticality != null ? criticality : Criticality.NORMAL;
        this.neverCorrect = neverCorrect;
        this.baseSeverityMap = baseSeverityMap != null ? baseSeverityMap : Collections.emptyMap();
        this.singleThreshold = singleThreshold;
        this.deadlineOverrideDays = deadlineOverrideDays;
    }

    public Criticality getCriticality() { return criticality; }
    public boolean isRedLine() { return criticality == Criticality.RED; }
    public boolean isNeverCorrect() { return neverCorrect; }
    public Map<String, Severity> getBaseSeverityMap() { return baseSeverityMap; }
    public SingleThreshold getSingleThreshold() { return singleThreshold; }
    public boolean hasSingleThreshold() { return singleThreshold != null; }
    public Integer getDeadlineOverrideDays() { return deadlineOverrideDays; }

    public Severity lookupBaseSeverity(String responseValue) {
        if (responseValue == null || baseSeverityMap.isEmpty()) return null;
        Severity exact = baseSeverityMap.get(responseValue);
        if (exact != null) return exact;
        for (Map.Entry<String, Severity> e : baseSeverityMap.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(responseValue)) return e.getValue();
        }
        return null;
    }

    public static ItemRule fromJson(String json) {
        if (json == null || json.isBlank()) return EMPTY;
        try {
            JsonNode n = MAPPER.readTree(json);

            Criticality crit = Criticality.NORMAL;
            String cs = n.path("criticality").asText(null);
            if ("RED".equalsIgnoreCase(cs)) crit = Criticality.RED;

            boolean never = n.path("neverCorrect").asBoolean(false);

            Map<String, Severity> map = new HashMap<>();
            if (n.has("baseSeverityMap") && n.get("baseSeverityMap").isObject()) {
                n.get("baseSeverityMap").fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    String val = entry.getValue().asText();
                    try { map.put(key, Severity.valueOf(val.toUpperCase())); }
                    catch (IllegalArgumentException ignored) {}
                });
            }
            if (n.has("forceCorrect") && n.get("forceCorrect").isArray()) {
                for (JsonNode v : n.get("forceCorrect")) {
                    if (v.isTextual()) map.put(v.asText(), Severity.HIGH);
                }
            }

            SingleThreshold st = null;
            if (n.has("singleThreshold") && n.get("singleThreshold").isObject()) {
                JsonNode stNode = n.get("singleThreshold");
                double sev = stNode.path("sevThreshold").asDouble(-1);
                if (sev >= 0 && sev <= 1) {
                    Severity ts = Severity.HIGH;
                    String tsStr = stNode.path("triggerSeverity").asText(null);
                    if (tsStr != null) {
                        try { ts = Severity.valueOf(tsStr.toUpperCase()); }
                        catch (IllegalArgumentException ignored) {}
                    }
                    st = new SingleThreshold(sev, ts);
                }
            }

            Integer deadlineDays = null;
            if (n.has("deadlineOverrideDays") && n.get("deadlineOverrideDays").isNumber()) {
                deadlineDays = n.get("deadlineOverrideDays").asInt();
            } else if (n.has("deadlineOverride") && n.get("deadlineOverride").isObject()) {
                deadlineDays = n.get("deadlineOverride").path("high").asInt(0);
                if (deadlineDays == 0) deadlineDays = null;
            }

            return new ItemRule(crit, never, map, st, deadlineDays);
        } catch (Exception e) {
            return EMPTY;
        }
    }

    public String toJson() {
        try {
            Map<String, Object> out = new HashMap<>();
            out.put("criticality", criticality.name());
            out.put("neverCorrect", neverCorrect);
            if (!baseSeverityMap.isEmpty()) {
                Map<String, String> m = new HashMap<>();
                baseSeverityMap.forEach((k, v) -> m.put(k, v.name()));
                out.put("baseSeverityMap", m);
            }
            if (singleThreshold != null) {
                Map<String, Object> st = new HashMap<>();
                st.put("sevThreshold", singleThreshold.getSevThreshold());
                st.put("triggerSeverity", singleThreshold.getTriggerSeverity().name());
                out.put("singleThreshold", st);
            }
            if (deadlineOverrideDays != null) out.put("deadlineOverrideDays", deadlineOverrideDays);
            return MAPPER.writeValueAsString(out);
        } catch (Exception e) {
            return "{}";
        }
    }

    public boolean isEmpty() {
        return criticality == Criticality.NORMAL && !neverCorrect
                && baseSeverityMap.isEmpty()
                && singleThreshold == null
                && deadlineOverrideDays == null;
    }
}
