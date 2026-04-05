package com.school.management.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 事件触发器引擎
 * 业务模块调用 fire(pointCode, context) 即可触发事件链
 * 设计要求: 绝不能因为触发器异常而影响调用方的业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 触发事件
     * @param pointCode 触发点编码
     * @param context   上下文数据
     */
    @Transactional
    public void fire(String pointCode, Map<String, Object> context) {
        try {
            List<Map<String, Object>> triggers = jdbcTemplate.queryForList(
                "SELECT * FROM event_triggers WHERE trigger_point_code = ? AND is_enabled = 1 AND deleted = 0",
                pointCode);

            for (Map<String, Object> trigger : triggers) {
                try {
                    processTrigger(trigger, context, pointCode);
                } catch (Exception e) {
                    log.warn("触发器执行失败 [{}]: {}", trigger.get("name"), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("触发器引擎异常 pointCode={}: {}", pointCode, e.getMessage());
        }
    }

    /**
     * 测试触发 - 不实际创建事件，返回匹配结果
     */
    public List<Map<String, Object>> testFire(String pointCode, Map<String, Object> context) {
        List<Map<String, Object>> triggers = jdbcTemplate.queryForList(
            "SELECT * FROM event_triggers WHERE trigger_point_code = ? AND is_enabled = 1 AND deleted = 0",
            pointCode);

        return triggers.stream()
            .filter(trigger -> {
                String condJson = trigger.get("condition_json") != null
                    ? trigger.get("condition_json").toString() : null;
                return ConditionMatcher.matches(condJson, context, objectMapper);
            })
            .map(trigger -> {
                String eventType = resolveEventType(trigger, context);
                String subjectType = (String) trigger.getOrDefault("subject_type", "USER");
                Long subjectId = toLong(context.get((String) trigger.get("subject_source")));
                String subjectNameField = (String) trigger.get("subject_name_source");
                String subjectName = subjectNameField != null
                    ? String.valueOf(context.getOrDefault(subjectNameField, "")) : "";

                return Map.<String, Object>of(
                    "triggerId", trigger.get("id"),
                    "triggerName", trigger.get("name"),
                    "eventType", eventType != null ? eventType : "",
                    "subjectType", subjectType,
                    "subjectId", subjectId != null ? subjectId : 0,
                    "subjectName", subjectName,
                    "matched", true
                );
            })
            .toList();
    }

    private void processTrigger(Map<String, Object> trigger, Map<String, Object> context, String pointCode) throws Exception {
        // 1. Check condition
        String condJson = trigger.get("condition_json") != null
            ? trigger.get("condition_json").toString() : null;
        if (!ConditionMatcher.matches(condJson, context, objectMapper)) return;

        // 2. Determine event type
        String eventType = resolveEventType(trigger, context);
        if (eventType == null || "null".equals(eventType) || eventType.isBlank()) return;

        // 3. Extract subject
        String subjectType = (String) trigger.getOrDefault("subject_type", "USER");
        Long subjectId = toLong(context.get((String) trigger.get("subject_source")));
        String subjectNameField = (String) trigger.get("subject_name_source");
        String subjectName = subjectNameField != null
            ? String.valueOf(context.getOrDefault(subjectNameField, "")) : "";
        if (subjectId == null) return;

        // 4. Get event type info
        String eventLabel = eventType;
        String eventCategory = "UNKNOWN";
        try {
            Map<String, Object> typeInfo = jdbcTemplate.queryForMap(
                "SELECT type_name, category_code FROM entity_event_types WHERE type_code = ? AND deleted = 0 LIMIT 1",
                eventType);
            eventLabel = (String) typeInfo.get("type_name");
            eventCategory = (String) typeInfo.get("category_code");
        } catch (Exception ignored) {}

        // 5. Build payload from context
        String payload = objectMapper.writeValueAsString(context);

        // 6. Get module from trigger point
        String module = "";
        try {
            module = jdbcTemplate.queryForObject(
                "SELECT module_code FROM trigger_points WHERE point_code = ? AND deleted = 0 LIMIT 1",
                String.class, pointCode);
        } catch (Exception ignored) {}

        // 7. Get current user
        Long userId = null;
        String userName = null;
        try {
            userId = SecurityUtils.getCurrentUserId();
            userName = SecurityUtils.getCurrentUsername();
        } catch (Exception ignored) {}

        // 8. Insert event
        jdbcTemplate.update(
            "INSERT INTO entity_events (tenant_id, subject_type, subject_id, subject_name, " +
            "event_category, event_type, event_label, payload, source_module, " +
            "created_by, created_by_name, occurred_at, created_at) " +
            "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
            subjectType, subjectId, subjectName,
            eventCategory, eventType, eventLabel, payload, module,
            userId, userName);

        log.info("事件触发: type={}, subject={}({}), trigger={}",
            eventType, subjectName, subjectType, trigger.get("name"));
    }

    private String resolveEventType(Map<String, Object> trigger, Map<String, Object> context) {
        String mode = String.valueOf(trigger.get("event_type_mode"));
        if ("DYNAMIC".equals(mode)) {
            String source = (String) trigger.get("event_type_source");
            Object val = context.get(source);
            return val != null ? String.valueOf(val) : null;
        } else {
            return (String) trigger.get("event_type_code");
        }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
