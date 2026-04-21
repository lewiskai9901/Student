package com.school.management.infrastructure.extension.plugins.healthcare.messaging.targetmode;

import com.school.management.infrastructure.extension.TargetModeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * BY_WARD — 按病区扩展消息接收者.
 *
 * config 形态: {"wardId": 123} 或 {"wardField": "wardId"} (从 event.payload 取);
 * 返回: 该病区下所有 in_ward 关系的 user_ids (病人 + 医护).
 *
 * 该模式由 HEALTH 插件贡献, core 没有. 用于"本病区通知"场景.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ByWardTargetMode implements TargetModeResolver {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String modeCode() { return "BY_WARD"; }

    @Override
    public String displayName() { return "按病区成员"; }

    @Override
    public List<Long> resolve(Map<String, Object> config, Map<String, Object> event) {
        try {
            if (config == null) return List.of();
            Long wardId = extractWardId(config, event);
            if (wardId == null) return List.of();

            // 查 access_relations 里 relation_code='in_ward' 且 resource_id=wardId 的所有 subject_user_id
            String sql = "SELECT DISTINCT subject_id FROM access_relations " +
                "WHERE relation_code = 'in_ward' " +
                "  AND subject_type = 'user' " +
                "  AND resource_type = 'org_unit' " +
                "  AND resource_id = ? " +
                "  AND deleted = 0";
            return jdbcTemplate.queryForList(sql, Long.class, wardId);
        } catch (Exception e) {
            log.warn("[BY_WARD] 查询失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean supportsPreview() {
        // 仅依赖 config.wardId, 不依赖 event, 支持静态预览
        return true;
    }

    private Long extractWardId(Map<String, Object> config, Map<String, Object> event) {
        Object direct = config.get("wardId");
        if (direct instanceof Number n) return n.longValue();
        if (direct instanceof String s && !s.isBlank()) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        // 若 config 指定从 event 取字段
        Object field = config.get("wardField");
        if (field instanceof String fname && event != null) {
            Object payload = event.get("payload");
            if (payload instanceof Map<?, ?> p) {
                Object v = p.get(fname);
                if (v instanceof Number n) return n.longValue();
                if (v instanceof String s && !s.isBlank()) {
                    try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
                }
            }
            Object top = event.get(fname);
            if (top instanceof Number n) return n.longValue();
        }
        return null;
    }
}
