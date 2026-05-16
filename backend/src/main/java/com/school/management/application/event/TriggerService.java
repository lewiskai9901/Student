package com.school.management.application.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.event.model.EntityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 事件触发器引擎
 * 业务模块调用 fire(pointCode, context) 即可触发事件链
 * 设计要求: 绝不能因为触发器异常而影响调用方的业务逻辑
 *
 * context 约定字段:
 *   _refType         (String) - 来源记录类型, 如 "inspection_record", "attendance"
 *   _refId           (Long)   - 来源记录ID
 *   _idempotencyKey  (String) - 业务幂等前缀；最终入库的 idempotency_key 为
 *                                "{prefix}:{triggerId}:{subjectType}:{subjectId}"
 *
 * 可观测性：所有静默跳过点都会以 WARN 记录，并带 pointCode/triggerId/缺失 key，
 *          避免消息链路静默失败难以定位。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TriggerPipelineHealthCheck healthCheck;

    private static final TypeReference<List<Map<String, String>>> SUBJECTS_TYPE_REF =
        new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> GENERIC_MAP_REF =
        new TypeReference<>() {};

    /**
     * 触发事件
     * @param pointCode 触发点编码
     * @param context   上下文数据
     */
    @Transactional
    public void fire(String pointCode, Map<String, Object> context) {
        if (!healthCheck.isHealthy()) {
            log.warn("[trigger] fire({}) skipped — pipeline unhealthy, missing: {}",
                pointCode, healthCheck.getMissingTables());
            return;
        }
        try {
            // 契约校验：trigger_points.context_schema 声明了业务代码必须传入的 key
            validateContextSchema(pointCode, context);

            List<Map<String, Object>> triggers = jdbcTemplate.queryForList(
                "SELECT * FROM event_triggers WHERE trigger_point_code = ? AND is_enabled = 1 AND plugin_enabled = 1 AND deleted = 0",
                pointCode);

            if (triggers.isEmpty()) {
                // 这是合法状态（没人订阅这个点），debug 即可
                log.debug("[trigger] point={} 无启用的触发器配置", pointCode);
                return;
            }

            for (Map<String, Object> trigger : triggers) {
                try {
                    processTrigger(trigger, context, pointCode);
                } catch (Exception e) {
                    log.warn("[trigger] 触发器执行失败 pointCode={}, triggerId={}, triggerName={}",
                        pointCode, trigger.get("id"), trigger.get("name"), e);
                }
            }
        } catch (Exception e) {
            log.error("[trigger] 引擎异常 pointCode={}", pointCode, e);
        }
    }

    /**
     * 测试触发 - 不实际创建事件，返回匹配结果
     */
    public List<Map<String, Object>> testFire(String pointCode, Map<String, Object> context) {
        List<Map<String, Object>> triggers = jdbcTemplate.queryForList(
            "SELECT * FROM event_triggers WHERE trigger_point_code = ? AND is_enabled = 1 AND plugin_enabled = 1 AND deleted = 0",
            pointCode);

        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> trigger : triggers) {
            String condJson = trigger.get("condition_json") != null
                ? trigger.get("condition_json").toString() : null;
            if (!ConditionMatcher.matches(condJson, context, objectMapper)) continue;

            String eventType = resolveEventType(trigger, context);
            List<Map<String, String>> subjects = parseSubjects(trigger);

            for (Map<String, String> subject : subjects) {
                Long subjectId = toLong(context.get(subject.get("idSource")));
                String nameField = subject.get("nameSource");
                String subjectName = nameField != null
                    ? String.valueOf(context.getOrDefault(nameField, "")) : "";

                results.add(Map.of(
                    "triggerId", trigger.get("id"),
                    "triggerName", trigger.get("name"),
                    "eventType", eventType != null ? eventType : "",
                    "subjectType", subject.getOrDefault("type", "USER"),
                    "subjectId", subjectId != null ? subjectId : 0,
                    "subjectName", subjectName,
                    "matched", true
                ));
            }
        }
        return results;
    }

    // ── 契约校验 ──────────────────────────────────────────────────

    /**
     * 读取 trigger_points.context_schema，校验必填 key 是否都在 context 里。
     * Schema 格式：
     *   {"required": ["batchId", "createdBy"], "description": "..."}
     * 缺失 key 时 WARN 记录（不抛异常，保持"不影响业务"语义），让调用方至少能被发现。
     */
    private void validateContextSchema(String pointCode, Map<String, Object> context) {
        Map<String, Object> schema;
        try {
            String schemaJson = jdbcTemplate.queryForObject(
                "SELECT context_schema FROM trigger_points WHERE point_code = ? AND deleted = 0 LIMIT 1",
                String.class, pointCode);
            if (schemaJson == null || schemaJson.isBlank()) {
                // 未声明 schema 视为不校验；但记一条 debug 便于后续完善
                log.debug("[trigger] pointCode={} 未声明 context_schema，跳过契约校验", pointCode);
                return;
            }
            schema = objectMapper.readValue(schemaJson, GENERIC_MAP_REF);
        } catch (Exception e) {
            log.debug("[trigger] pointCode={} 读取 context_schema 失败，跳过校验: {}",
                pointCode, e.getMessage());
            return;
        }

        Object required = schema.get("required");
        if (!(required instanceof List<?> requiredList) || requiredList.isEmpty()) return;

        List<String> missing = new ArrayList<>();
        for (Object keyObj : requiredList) {
            String key = String.valueOf(keyObj);
            if (!context.containsKey(key) || context.get(key) == null) {
                missing.add(key);
            }
        }
        if (!missing.isEmpty()) {
            log.warn("[trigger] 契约违反 pointCode={}, missingKeys={}, providedKeys={}. "
                    + "业务代码传入的 context 缺少 trigger_points.context_schema.required 声明的字段，"
                    + "相关触发器会因 subjectId/idSource 找不到而跳过。",
                pointCode, missing,
                context.keySet());
        }
    }

    // ── 单个 trigger 处理 ──────────────────────────────────────────

    private void processTrigger(Map<String, Object> trigger, Map<String, Object> context, String pointCode) throws Exception {
        Long triggerId = toLong(trigger.get("id"));
        String triggerName = (String) trigger.get("name");

        // 1. Check condition
        String condJson = trigger.get("condition_json") != null
            ? trigger.get("condition_json").toString() : null;
        if (!ConditionMatcher.matches(condJson, context, objectMapper)) {
            log.debug("[trigger] pointCode={} triggerId={} 条件不匹配，跳过", pointCode, triggerId);
            return;
        }

        // 2. Determine event type
        String eventType = resolveEventType(trigger, context);
        if (eventType == null || "null".equals(eventType) || eventType.isBlank()) {
            log.warn("[trigger] pointCode={} triggerId={}({}) eventType 解析为空 "
                    + "(mode={}, source={}, code={})，跳过事件写入。",
                pointCode, triggerId, triggerName,
                trigger.get("event_type_mode"),
                trigger.get("event_type_source"),
                trigger.get("event_type_code"));
            return;
        }

        // 3. Parse subjects array
        List<Map<String, String>> subjects = parseSubjects(trigger);
        if (subjects.isEmpty()) {
            log.warn("[trigger] pointCode={} triggerId={}({}) subjects_json 为空或解析失败，"
                    + "无法确定事件主体，跳过。",
                pointCode, triggerId, triggerName);
            return;
        }

        // 4. Get event type info (shared across all subjects)
        String eventLabel = eventType;
        String eventCategory = "UNKNOWN";
        try {
            Map<String, Object> typeInfo = jdbcTemplate.queryForMap(
                "SELECT type_name, category_code FROM entity_event_types WHERE type_code = ? AND deleted = 0 LIMIT 1",
                eventType);
            eventLabel = (String) typeInfo.get("type_name");
            eventCategory = (String) typeInfo.get("category_code");
        } catch (Exception e) {
            log.warn("[trigger] eventType={} 在 entity_event_types 中未注册，将以 UNKNOWN 分类写入。"
                    + " 请补全事件类型注册。", eventType);
        }

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

        // 8. Extract source_ref from context convention
        String refType = context.get("_refType") != null ? context.get("_refType").toString() : null;
        Long refId = toLong(context.get("_refId"));

        // 9. Idempotency prefix — 业务可选传入；最终 key 拼接 triggerId + subject 维度，
        //    保证同一业务操作对每个 trigger/subject 只产生一条事件。
        //    M4.2: 业务未传 _idempotencyKey 时自动按严格 hash (无时间戳) 生成前缀,
        //    保守选"严格幂等": 同一 (triggerPoint + _refType + _refId) 反复 fire 去重.
        //    若业务场景确需"每次都发", 请显式传 _idempotencyKey=时间戳或 UUID.
        String idempotencyPrefix = context.get("_idempotencyKey") != null
            ? context.get("_idempotencyKey").toString() : null;
        if (idempotencyPrefix == null || idempotencyPrefix.isBlank()) {
            idempotencyPrefix = autoIdempotencyPrefix(pointCode, context);
        }

        // 10. Insert one event per subject + publish Spring event for notification dispatch
        int inserted = 0;
        int skipped = 0;
        for (Map<String, String> subject : subjects) {
            String subjectType = subject.getOrDefault("type", "USER");
            String idSource = subject.get("idSource");
            Long subjectId = toLong(context.get(idSource));
            if (subjectId == null) {
                // 这是最重要的告警点：业务代码承诺的 context key 在 subjects_json.idSource 里没找到
                log.warn("[trigger] pointCode={} triggerId={}({}) 主体 subjectType={} 的 idSource='{}' "
                        + "在 context 中缺失或为 null，跳过该主体。providedKeys={}",
                    pointCode, triggerId, triggerName,
                    subjectType, idSource,
                    context.keySet());
                skipped++;
                continue;
            }

            String nameField = subject.get("nameSource");
            String subjectName = (nameField != null && !nameField.isBlank())
                ? String.valueOf(context.getOrDefault(nameField, "")) : "";

            // 构造最终幂等 key
            String idemKey = idempotencyPrefix != null
                ? String.format("%s:%d:%s:%d", idempotencyPrefix, triggerId, subjectType, subjectId)
                : null;

            try {
                jdbcTemplate.update(
                    "INSERT INTO entity_events (tenant_id, subject_type, subject_id, subject_name, " +
                    "event_category, event_type, event_label, idempotency_key, payload, source_module, " +
                    "source_ref_type, source_ref_id, " +
                    "created_by, created_by_name, occurred_at, created_at) " +
                    "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                    subjectType, subjectId, subjectName,
                    eventCategory, eventType, eventLabel, idemKey, payload, module,
                    refType, refId,
                    userId, userName);
            } catch (DuplicateKeyException dup) {
                // 幂等命中：同一 idempotency_key 已存在，静默跳过（但记 info，方便观察）
                log.info("[trigger] 幂等命中，事件已存在跳过: idempotencyKey={}, eventType={}",
                    idemKey, eventType);
                continue;
            }

            // Publish Spring event → EntityEventDispatchListener → MessageDispatcher
            EntityEvent domainEvent = EntityEvent.builder()
                .tenantId(1L)
                .subjectType(subjectType).subjectId(subjectId).subjectName(subjectName)
                .eventCategory(eventCategory).eventType(eventType).eventLabel(eventLabel)
                .payload(payload).sourceModule(module)
                .sourceRefType(refType).sourceRefId(refId)
                .createdBy(userId).createdByName(userName)
                .build();
            eventPublisher.publishEvent(new EntityEventCreatedNotification(domainEvent));
            inserted++;
        }

        log.info("[trigger] pointCode={} triggerId={}({}) eventType={} inserted={} skipped={}",
            pointCode, triggerId, triggerName, eventType, inserted, skipped);
    }

    // ── 工具方法 ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseSubjects(Map<String, Object> trigger) {
        Object raw = trigger.get("subjects_json");
        if (raw == null) return List.of();
        try {
            String json = raw.toString();
            return objectMapper.readValue(json, SUBJECTS_TYPE_REF);
        } catch (Exception e) {
            log.warn("[trigger] 解析 subjects_json 失败 triggerId={}: {}",
                trigger.get("id"), e.getMessage());
            return List.of();
        }
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

    /**
     * M4.2: 自动生成幂等前缀.
     *
     * 策略: 纯业务字段 hash (不加时间戳) — 严格幂等.
     *   basis = pointCode + _refType + _refId
     *   (如 _refType/_refId 缺失, 退化到 pointCode + 全 context 的字段名 sorted hash)
     *
     * 效果: 同一业务对象对同一触发点反复 fire, 只会记录一次事件;
     *       业务如果想每次都产生事件, 应显式传入 _idempotencyKey=UUID/timestamp.
     */
    private String autoIdempotencyPrefix(String pointCode, Map<String, Object> context) {
        Object refType = context.get("_refType");
        Object refId = context.get("_refId");
        String basis;
        if (refType != null && refId != null) {
            basis = pointCode + ":" + refType + ":" + refId;
        } else {
            // 没有来源记录 ID 时, 用 subject 相关字段兜底
            basis = pointCode + ":"
                + context.getOrDefault("subjectId", "")
                + ":" + context.getOrDefault("subjectType", "USER")
                + ":" + context.getOrDefault("studentId", "")
                + ":" + context.getOrDefault("userId", "");
        }
        return "AUTO-" + Integer.toHexString(basis.hashCode());
    }
}
