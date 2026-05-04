package com.school.management.domain.inspection.correction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * RISK_MATRIX 打分模式 normalizer.
 *
 * <p>scoringConfig 期望结构:
 * <pre>
 * {
 *   "matrix": [
 *     [{"level":"L"},{"level":"L"},{"level":"M"},{"level":"H"}],
 *     [{"level":"L"},{"level":"M"},{"level":"M"},{"level":"H"}],
 *     [{"level":"M"},{"level":"M"},{"level":"H"},{"level":"VH"}],
 *     [{"level":"M"},{"level":"H"},{"level":"VH"},{"level":"VH"}]
 *   ],
 *   "levelToSeverity": {"L":0.0,"M":0.4,"H":0.75,"VH":1.0}
 * }
 * </pre>
 *
 * <p>responseValue 期望: 形如 "2,3" (probability=2, impact=3, 0-based) 或 JSON
 * <code>{"probability":2,"impact":3}</code>.
 *
 * <p>解析失败 / 越界 → 返回 null (跳过判定).
 */
public class RiskMatrixNormalizer implements SeverityNormalizer {

    private static final Logger log = LoggerFactory.getLogger(RiskMatrixNormalizer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 默认 level→severity 映射 (cfg 未指定时使用). */
    private static final Map<String, Double> DEFAULT_LEVEL_MAP = new HashMap<>();
    static {
        DEFAULT_LEVEL_MAP.put("L",  0.0);
        DEFAULT_LEVEL_MAP.put("M",  0.4);
        DEFAULT_LEVEL_MAP.put("H",  0.75);
        DEFAULT_LEVEL_MAP.put("VH", 1.0);
        DEFAULT_LEVEL_MAP.put("E",  1.0);  // Extreme 别名
    }

    @Override
    public Double normalize(SubmissionDetail detail, BigDecimal itemWeight) {
        String resp = detail.getResponseValue();
        if (resp == null || resp.isBlank()) return null;

        int probIdx, impactIdx;
        try {
            int[] coords = parseCoords(resp);
            probIdx = coords[0];
            impactIdx = coords[1];
        } catch (Exception e) {
            log.debug("RISK_MATRIX parse coords failed: {}", resp);
            return null;
        }

        String cfgJson = detail.getScoringConfig();
        JsonNode cfg = null;
        if (cfgJson != null && !cfgJson.isBlank()) {
            try { cfg = MAPPER.readTree(cfgJson); }
            catch (Exception ignored) {}
        }

        // 取矩阵 level 字符串
        String level = lookupLevel(cfg, probIdx, impactIdx);
        if (level == null) return null;

        // 取 levelToSeverity 映射
        Map<String, Double> map = DEFAULT_LEVEL_MAP;
        if (cfg != null && cfg.has("levelToSeverity") && cfg.get("levelToSeverity").isObject()) {
            JsonNode lts = cfg.get("levelToSeverity");
            Map<String, Double> custom = new HashMap<>();
            lts.fields().forEachRemaining(entry -> {
                JsonNode v = entry.getValue();
                if (v.isNumber()) custom.put(entry.getKey().toUpperCase(), v.doubleValue());
            });
            map = custom;
        }

        Double sev = map.get(level.toUpperCase());
        if (sev == null) sev = DEFAULT_LEVEL_MAP.get(level.toUpperCase());
        if (sev == null) return null;
        if (sev < 0) sev = 0.0;
        if (sev > 1) sev = 1.0;
        return sev;
    }

    private static int[] parseCoords(String resp) throws Exception {
        resp = resp.trim();
        // JSON 格式
        if (resp.startsWith("{")) {
            JsonNode n = MAPPER.readTree(resp);
            int p = n.path("probability").asInt(-1);
            int i = n.path("impact").asInt(-1);
            if (p < 0 || i < 0) throw new IllegalArgumentException("missing coords");
            return new int[]{p, i};
        }
        // CSV 格式 "p,i"
        String[] parts = resp.split(",");
        if (parts.length != 2) throw new IllegalArgumentException("bad coord format");
        return new int[]{
                Integer.parseInt(parts[0].trim()),
                Integer.parseInt(parts[1].trim())
        };
    }

    private static String lookupLevel(JsonNode cfg, int probIdx, int impactIdx) {
        if (cfg == null || !cfg.has("matrix")) return null;
        JsonNode matrix = cfg.get("matrix");
        if (!matrix.isArray() || probIdx < 0 || probIdx >= matrix.size()) return null;
        JsonNode row = matrix.get(probIdx);
        if (!row.isArray() || impactIdx < 0 || impactIdx >= row.size()) return null;
        JsonNode cell = row.get(impactIdx);
        if (cell.isTextual()) return cell.asText();
        if (cell.isObject() && cell.has("level")) return cell.get("level").asText();
        return null;
    }
}
